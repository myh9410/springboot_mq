# 2026-05-23 — Consumer 1차 구현 (시나리오 A-1, String 메시지)

> 1차 draft. 추후 다시 정리할 예정.

## 1. 목표

지난 라운드에 만든 Producer가 보낸 메시지를 같은 앱 안의 Consumer가 받아 처리하도록 한다. 메시지 타입은 여전히 `String` (A-1). End-to-end가 동작하면 시나리오 A 종료.

## 2. 결정 사항

| 항목 | 선택 | 이유 |
|---|---|---|
| 패키지 | `io.github.myh9410.mq.consumer` | Producer와 대칭 |
| 클래스 | `KafkaConsumerConfig`, `MessageConsumer` | Producer 측 네이밍 패턴 동일 |
| group-id | `springboot-mq-consumer` (yml) | 환경 의존성에 가까워서 yml에 둠 |
| auto-offset-reset | `earliest` (yml) | 학습 편의: 새 group은 토픽 시작부터 다 읽어줌 |
| 역직렬화 | `StringDeserializer` (key/value, Java config) | Producer와 대칭 |
| 토픽 참조 | MessageConsumer 내부 local 상수 `"messages"` | 첫 단계에서는 단순 유지. 토픽이 늘어나면 공용 `Topics` 클래스로 리팩토링 |

## 3. 작업 결과

### 패키지 구조 (이번 단계 추가분)

```
io.github.myh9410.mq/
└── consumer/
    ├── KafkaConsumerConfig.java   # ConsumerFactory + ListenerContainerFactory 빈
    └── MessageConsumer.java       # @KafkaListener
```

### application.yml 추가

```yaml
spring:
  kafka:
    consumer:
      group-id: springboot-mq-consumer
      auto-offset-reset: earliest
```

### KafkaConsumerConfig — Producer config와 대칭 구조

```java
@Bean
public ConsumerFactory<String, String> consumerFactory(KafkaProperties kafkaProperties) {
    Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
    config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(config);
}

@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(...) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, String>();
    factory.setConsumerFactory(consumerFactory);
    return factory;
}
```

Producer의 `buildProducerProperties()` 와 동일한 패턴: yml에서 `spring.kafka.consumer.*` 값을 읽어 `Map<String, Object>` 로 받은 뒤 deserializer만 코드로 덮어쓴다.

### MessageConsumer

```java
@KafkaListener(topics = TOPIC)
public void onMessage(ConsumerRecord<String, String> record) {
    log.info("Received: topic={} partition={} offset={} key={} payload={}",
        record.topic(), record.partition(), record.offset(), record.key(), record.value());
}
```

핸들러 시그니처를 `String payload` 하나로 받지 않고 `ConsumerRecord<String, String>` 통째로 받는 이유: **partition, offset, headers, key, timestamp 등 메타데이터까지 한 번에 접근 가능**. 학습용으로는 메타가 보이는 게 훨씬 유용하다.

## 4. 학습 포인트

### 4.1 Consumer Group과 offset 시작점 — `auto-offset-reset` 의미

같은 토픽을 여러 컨슈머 그룹이 각자의 위치(offset)로 읽을 수 있다. **broker는 `<topic, partition, group-id>` 단위로 "어디까지 읽었는지(committed offset)" 를 기억**해준다 (`__consumer_offsets` 내부 토픽).

새 group이 처음 토픽에 붙으면 committed offset이 없으므로 어디서 시작할지 결정해야 하는데, 그게 `auto.offset.reset`:

| 값 | 동작 | 언제 |
|---|---|---|
| `earliest` | 토픽 맨 처음부터 | 학습 / 모든 이력을 받아야 하는 컨슈머 |
| `latest` (기본값) | 붙은 시점 이후 새로 들어오는 것만 | 실시간 처리만 필요한 경우 |
| `none` | 시작점 없으면 예외 | 안전망 모드 |

이번 라운드에서 실제로 확인: 새 group `springboot-mq-consumer` 가 처음 붙자마자 지난 세션에 남아있던 offset=0 메시지를 받아왔다.

```
Received: topic=messages partition=0 offset=0 payload=hello kafka from springboot_mq
```

이건 **broker가 단순한 fire-and-forget 채널이 아니라, 메시지를 "보관해두는 로그"** 라는 사실을 처음 체감하는 지점이다 (Kafka의 핵심 정신).

### 4.2 `@EnableKafka` 는 따로 안 붙여도 되나

Spring Kafka에서 `@KafkaListener` 가 동작하려면 `@EnableKafka` 가 필요하다. 그런데 Spring Boot Kafka starter가 `KafkaAnnotationDrivenConfiguration` 자동설정을 통해 **이미 enable 해놓은 상태**이기 때문에 우리가 직접 붙일 필요가 없다.

옛 자료나 Boot 안 쓰는 순수 Spring 코드는 `@EnableKafka` 가 명시적으로 보일 수 있다 — 둘의 차이를 기억해두면 좋다.

### 4.3 `@KafkaListener` 의 `containerFactory` 인자

지금은 생략했는데, 사실 `@KafkaListener` 는 어떤 listener container factory 빈을 쓸지 선택할 수 있다:

```java
@KafkaListener(topics = TOPIC, containerFactory = "kafkaListenerContainerFactory")
```

생략하면 기본값이 `"kafkaListenerContainerFactory"` 다 — 그래서 **우리가 만든 빈 이름을 정확히 이 이름으로 둔 것**. 만약 빈 이름을 다르게 했다면 `containerFactory = "..."` 로 명시했어야 한다.

용도별 factory 여러 개를 쓰는 패턴은 시나리오 B/C 쯤에서 다룰 예정.

### 4.4 스레드 풀 분리 관찰

종단간 한 번 보내면 로그에서 세 가지 스레드가 보인다:

| 스레드 | 역할 |
|---|---|
| `[nio-8080-exec-1]` | HTTP 요청 처리 (Tomcat NIO worker) |
| `[t-mq-producer-1]` | Producer send 결과 callback (`whenComplete`) |
| `[ntainer#0-0-C-1]` | Consumer 메시지 수신 (Kafka listener container) |

→ **요청 처리, 송신 결과 처리, 메시지 소비가 각각 별도 풀에서 동시에 진행**된다는 점이 명확히 보인다. 운영에서 한쪽이 느려져도 다른 쪽은 영향 없는 격리 구조의 출발점.

### 4.5 Consumer 수신과 Producer ack 콜백의 순서

흥미로운 관찰: 같은 메시지에 대해 **Consumer 수신 로그가 Producer의 "Sent ok" 콜백보다 먼저 찍힐 때가 있다**.

```
19:09:54.683  Sending: ...                (HTTP 스레드, send 호출 직전)
19:09:54.833  Received: ... offset=1      (consumer 스레드)
19:09:54.834  Sent ok: ... offset=1       (producer 콜백 스레드)
```

이유: broker가 메시지를 받아 partition에 쓰는 그 순간, 두 가지가 동시에 트리거된다 — (a) producer에게 ack 응답 전송, (b) 해당 partition을 subscribe 중인 consumer에게 fetch 응답. 둘은 별도 네트워크 호출이고 별도 스레드가 처리하므로, 어느 쪽이 먼저 로그에 찍힐지는 그날 그날의 스레드 스케줄링 운에 따른다.

→ "Producer가 ack 받는다 = Consumer가 메시지 받는다" 가 동시점에 일어난다고 봐도 무방하다는 학습 포인트.

## 5. End-to-End 검증 절차

```bash
# 1) Kafka 인프라
cd docker && docker compose up -d

# 2) 앱 실행
./gradlew bootRun

# 3) 메시지 발사
curl -X POST http://localhost:8080/messages \
  -H "Content-Type: text/plain" \
  -d "consumer test message"
# -> HTTP/1.1 202

# 4) 앱 로그에서 producer + consumer 모두 확인
# Sending: topic=messages payload=consumer test message
# Received: topic=messages partition=0 offset=N payload=consumer test message
# Sent ok: topic=messages partition=0 offset=N
```

## 6. 시나리오 A 종료 / 남은 TODO

시나리오 A-1 (String + 단방향 round-trip) 종료. 다음 후보:

- [ ] **A-2: JSON DTO 업그레이드** — `JsonSerializer` / `JsonDeserializer`, type info 헤더, 키 기반 파티셔닝
- [ ] Topics 공용 상수 클래스로 추출 (`io.github.myh9410.mq.Topics`) — 토픽 2개 이상부터 가치
- [ ] consumer group을 2개로 늘려서 "같은 메시지를 두 group이 독립적으로 다 받는다" 관찰
- [ ] 파티션 수를 늘리고 partition key 다르게 해서 어느 consumer 스레드(`ntainer#0-N-C-1`)에 분배되는지 관찰