# 2026-06-03 — B-2: ProducerListener로 send 결과 로깅 통합

> 1차 draft. 추후 다시 정리할 예정.

## 1. 목표

`MessageProducer.send()` 안에서 매번 `.whenComplete(...)` 콜백을 다는 패턴을, `KafkaTemplate`에 한 번만 부착하는 `ProducerListener` 로 옮긴다. 결과:
- 모든 producer 호출 지점이 같은 형식으로 로깅됨
- 애플리케이션 코드는 "보낸다" 의도만 표현, 결과 처리는 framework 레이어
- 새 producer 클래스가 추가돼도 로깅 패턴이 자동 일관

## 2. Before / After

### Before (B-1까지)

```java
// MessageProducer.send()
kafkaTemplate.send(TOPIC, key, message)
    .whenComplete((result, ex) -> {
        if (ex == null) {
            log.info("Sent ok: ...");
        } else {
            log.error("Send failed: ...", ex);
        }
    });
```

같은 코드를 producer 클래스마다 반복 → 어느 한 곳에서 로깅을 빼먹으면 silent failure.

### After (B-2)

```java
// MessageProducer.send() — 의도만 선언
log.info("Sending: ...");
kafkaTemplate.send(TOPIC, key, message);

// KafkaProducerConfig.kafkaTemplate() — 모든 send 결과 일괄 처리
template.setProducerListener(new ProducerListener<>() {
    @Override
    public void onSuccess(ProducerRecord<String, Object> record, RecordMetadata metadata) {
        log.info("Sent ok: topic={} partition={} offset={} key={}", ...);
    }
    @Override
    public void onError(ProducerRecord<String, Object> record, RecordMetadata metadata, Exception exception) {
        log.error("Send failed: topic={} key={}", ..., exception);
    }
});
```

## 3. 동작 확인

```
[nio-8080-exec-1] i.g.myh9410.mq.producer.MessageProducer       : Sending: topic=messages key=b2-test payload=...
[t-mq-producer-1] i.g.m.mq.producer.KafkaProducerConfig         : Sent ok: topic=messages partition=2 offset=5 key=b2-test
[ntainer#1-2-C-1] i.g.myh9410.mq.consumer.MessageConsumer       : Received: ...
```

"Sent ok" 로그의 logger 이름이 `MessageProducer` → `KafkaProducerConfig`로 옮겨진 게 핵심 차이. 스레드도 producer 콜백 풀(`t-mq-producer-1`) 그대로.

## 4. 트레이드오프

| 측면 | 장점 | 약점 |
|---|---|---|
| 일관성 | 모든 send가 동일 형식으로 로깅 | 모든 send가 똑같이 처리됨 — 특정 send만 특별 처리 못 함 |
| 추적성 | 어느 호출 코드든 결과 로그 보장 | 호출 컨텍스트(어떤 service에서 보냈는지)는 별도 헤더/MDC로 전달해야 보임 |
| 보일러플레이트 | producer 클래스에서 콜백 boilerplate 제거 | 결과를 비동기로 받아 후속 처리해야 한다면 여전히 `.whenComplete` 필요 |

→ **로깅은 listener로**, **send 후 비즈니스 후속 처리(예: send 성공 시 DB 상태 변경)는 호출 측 `.whenComplete`** 로 나눠지는 게 자연스러움. 이번 단계에서는 후속 비즈 처리가 없으므로 호출 측은 깔끔하게 `send`만 호출.

## 5. 다른 미세 디테일

- `RecordMetadata`는 `onError`에서 `null`일 수 있다 (브로커 도달 전 실패 케이스). 사용 시 null-check 필요.
- 예외는 `Exception` 으로 넘어옴 (`Throwable` 아님). `RuntimeException`인 경우가 보통.
- 같은 `ProducerListener` 인스턴스가 모든 send 호출에서 공유되므로 listener 안에 mutable state를 쓰지 말 것.

## 6. 다음

- D-2: 같은 패턴(여러 ContainerFactory)을 batch 처리에 적용
