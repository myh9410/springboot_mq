# 2026-06-03 — C: DefaultErrorHandler + DLQ

> 1차 draft. 추후 다시 정리할 예정.

## 1. 목표

컨슈머가 실패할 수 있는 두 가지 큰 갈래 — **(a) 리스너 메서드가 예외를 던질 때**, **(b) 메시지 자체가 역직렬화 안 될 때** — 양쪽을 **컨슈머가 안 죽고** 안전하게 처리하고, 결국 복구 불가능한 record는 별도 DLQ 토픽에 보존하는 패턴을 구축한다.

## 2. 핵심 구성요소

| 컴포넌트 | 역할 |
|---|---|
| `ErrorHandlingDeserializer` | `JacksonJsonDeserializer`를 감싼다. 역직렬화 실패해도 throw 대신 record header에 예외를 실어둔다 → 컨슈머가 죽지 않는다 |
| `DefaultErrorHandler` | 리스너가 throw하거나 deser 헤더가 있는 record를 받으면 backoff에 따라 재시도, 한계 초과 시 recoverer로 위임 |
| `ExponentialBackOff` | 재시도 간격 정책. `initial → multiplier → maxInterval → maxElapsedTime` 4개 파라미터로 제어 |
| `DeadLetterPublishingRecoverer` | 복구 불가 판정된 record를 DLQ 토픽에 publish + 원본 메타데이터를 헤더로 자동 첨부 |
| DLQ 전용 `KafkaTemplate` | DLQ 토픽 송신만 담당. 메인 producer와 분리해서 운영 격리 |

## 3. 핵심 코드 발췌

```java
// 1) ErrorHandlingDeserializer로 JacksonJsonDeserializer 래핑
JacksonJsonDeserializer<Message> jsonDeserializer =
    new JacksonJsonDeserializer<>(Message.class, jsonMapper, false);
ErrorHandlingDeserializer<Message> valueDeserializer =
    new ErrorHandlingDeserializer<>(jsonDeserializer);

// 2) ExponentialBackOff (학습용 짧은 값)
ExponentialBackOff backOff = new ExponentialBackOff();
backOff.setInitialInterval(500L);
backOff.setMultiplier(2.0);
backOff.setMaxInterval(2_000L);
backOff.setMaxElapsedTime(5_000L);    // 5초 후 DLQ로 위임

// 3) DLQ 토픽으로 라우팅하는 recoverer
DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
    dltKafkaTemplate,
    (record, ex) -> new TopicPartition(Topics.DLQ, -1)  // -1 = 브로커가 partition 결정
);
return new DefaultErrorHandler(recoverer, backOff);
```

## 4. 헤매기 쉬운 함정 정리

### 4.1 DLQ KafkaTemplate의 serializer 선택 — `StringSerializer`로는 안 된다

처음에 grip-marketing 패턴(`KafkaTemplate<String, String>` + `StringSerializer`)을 그대로 따라했더니 다음 에러:

```
ClassCastException: class io.github.myh9410.mq.message.Message cannot be cast to class java.lang.String
```

원인: **리스너가 throw한 경우**, recoverer가 DLQ에 보내는 value는 **이미 역직렬화된 도메인 객체(`Message`)**다. raw bytes가 아니다. 그래서 `StringSerializer`로는 캐스팅이 안 된다.

해결: DLQ 템플릿도 `JacksonJsonSerializer`로 — 다시 JSON으로 직렬화해서 보낸다. `Message` 객체 → JSON 문자열로 떨어진다.

```java
JacksonJsonSerializer<Object> valueSerializer = new JacksonJsonSerializer<>(jsonMapper);
valueSerializer.setAddTypeInfo(false);
new DefaultKafkaProducerFactory<>(config, new StringSerializer(), valueSerializer);
```

> grip-marketing은 컨슈머 자체가 `StringDeserializer`로 raw payload를 받고 listener 안에서 직접 Jackson으로 파싱하는 구조라 DLQ도 String/String이 자연스러웠던 것. 우리처럼 `JacksonJsonDeserializer`가 typed 객체로 변환해주는 구조에서는 DLQ 측도 JSON 직렬화가 맞다.

### 4.2 역직렬화 실패의 DLQ payload는 base64로 인코딩된다

`malformed-001 → "not a json"` 같은 raw bytes를 보냈을 때 DLQ에 떨어진 record:

```
key=malformed-001
payload="bm90IGEganNvbg=="    ← "not a json"의 base64
```

이유: `JsonSerializer`는 byte[]를 받으면 JSON 표준에 따라 base64 인코딩된 문자열로 직렬화한다. ErrorHandlingDeserializer가 실패 시 원본 byte[]를 다음 단계로 넘겨주는데, DLQ producer가 그걸 byte[]로 보고 base64화하는 흐름.

→ DLQ 운영 관점에서는 **원본 raw bytes를 그대로 보고 싶다면 `ByteArraySerializer`** 가 더 적합. 다만 정상 throw 케이스(typed 객체)와의 처리가 분기되어야 한다. 학습 단계에서는 단일 직렬화기로 통일하는 게 단순.

### 4.3 retry 간격 invariant

`maxElapsedTime` 이 `max.poll.interval.ms` (기본 5분) 보다 작아야 한다. 안 그러면:

1. 컨슈머가 retry 중에 5분 넘게 poll 안 함
2. 브로커가 컨슈머 죽었다고 판단 → 다른 컨슈머로 partition 재배정 (rebalance)
3. 재배정된 컨슈머가 다시 같은 record부터 처리 → 또 retry
4. 무한 rebalance + retry 루프

학습용 5초 설정에서는 문제 없지만, 운영에서 backoff를 길게 잡으려면 `MAX_POLL_INTERVAL_MS_CONFIG`도 함께 늘려야 한다.

## 5. 동작 검증 결과

### Scenario 1 — 정상 OK 메시지

```
content=hello
→ Received: ok-001 ... (정상 처리, DLQ 없음)
```

### Scenario 2 — 리스너 throw (transient 가정)

```
content=fail:induced
→ Received offset=1 (1차)
→ Received offset=1 (재시도 0.5s 후)
→ Received offset=1 (재시도 1s 후)
→ Received offset=1 (재시도 2s 후)
→ Received offset=1 (재시도 2s 후)
→ DLQ record: payload={"id":"fail-001","content":"fail:induced",...}
   headers: exception-fqcn=ListenerExecutionFailedException
            cause-fqcn=IllegalStateException
            exception-message=induced consumer failure for content=fail:induced
```

총 5번 처리 (1 + 4 retry). exponential backoff 패턴 그대로.

### Scenario 3 — 역직렬화 실패

```
입력: "not a json" (key=malformed-001)
→ 재시도 없이 즉시 DLQ (DeserializationException은 retryable 아님)
→ DLQ record: payload="bm90IGEganNvbg==" (base64)
   headers: exception-fqcn=DeserializationException
            exception-message=failed to deserialize
```

`DefaultErrorHandler`는 `DeserializationException`을 자동으로 **non-retryable**로 인식하고 backoff 없이 바로 recoverer로 위임한다 (재시도해도 결과가 같으니까).

## 6. 자동 첨부되는 DLQ 헤더 목록

`DeadLetterPublishingRecoverer`가 기본으로 추가하는 헤더(prefix `kafka_dlt-`):

| 헤더 | 의미 |
|---|---|
| `kafka_dlt-original-topic` | 원본 토픽 이름 |
| `kafka_dlt-original-partition` | 원본 partition (binary int) |
| `kafka_dlt-original-offset` | 원본 offset (binary long) |
| `kafka_dlt-original-timestamp` | 원본 timestamp |
| `kafka_dlt-original-timestamp-type` | `CreateTime` / `LogAppendTime` |
| `kafka_dlt-original-consumer-group` | 어떤 group의 consumer가 실패했는가 |
| `kafka_dlt-exception-fqcn` | 던져진 예외의 FQCN |
| `kafka_dlt-exception-cause-fqcn` | root cause의 FQCN |
| `kafka_dlt-exception-message` | 예외 메시지 |
| `kafka_dlt-exception-stacktrace` | 스택 트레이스 (꽤 김) |

partition/offset 같은 binary 헤더는 UTF-8 String으로 해석하면 깨져 보인다 — kafka-console-consumer로 볼 때 별도 디코딩이 필요. (DlqConsumer 로그에 보이는 `\x00\x00...` 글자가 그 이유.)

## 7. 추가 학습 컴포넌트

이번에 두 가지 패턴이 같이 들어왔다:

### `stringKafkaListenerContainerFactory` — 용도별 ContainerFactory 분리

DLQ를 관찰하려면 String/String 컨테이너가 필요해서 별도 빈을 추가했다:

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> stringKafkaListenerContainerFactory(...)
```

`@KafkaListener`에서 `containerFactory="stringKafkaListenerContainerFactory"` 로 명시 선택. 같은 앱에서 **여러 타입의 listener를 공존**시키는 표준 패턴이다 (D-2의 batch listener에서 또 쓰일 예정).

### `DlqConsumer` — DLQ 자체를 listen

학습용으로 DLQ에 떨어진 record를 즉시 로그로 보기 위해 별도 listener를 띄웠다. 실무에서는 DLQ를 **자동 처리하지 말고** 운영자가 수동으로 보고 판단하는 게 정석 (실패 원인 분석 → 데이터 수정 → 메인 토픽 재발행, 또는 영구 폐기).

## 8. 남은 TODO

- [ ] D-1: partition을 늘리고 컨슈머를 2개 띄워서 partition 재배분 동작 관찰
- [ ] 운영 관점에서 DLQ 모니터링 — Micrometer counter로 DLQ 발행 빈도 측정 (E 단계)
- [ ] 재시도 중인 사이 컨슈머가 종료되면 어떻게 되는가? (offset commit과 retry의 상호작용)
