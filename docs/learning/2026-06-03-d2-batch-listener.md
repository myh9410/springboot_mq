# 2026-06-03 — D-2: Batch Listener

> 1차 draft. 추후 다시 정리할 예정.

## 1. 목표

지금까지의 `@KafkaListener`는 record 한 건씩 받는 패턴이었다. 다운스트림 API가 batch 호출을 지원하는 경우(예: 외부 마케팅 API가 한 번에 30건까지 받음) 한 record씩 호출하면 비효율이 큼. **`setBatchListener(true)`** + `List<ConsumerRecord<...>>` 시그니처로 묶음 처리하는 패턴을 도입.

## 2. 추가 구성

### 2.1 별도 `batchConsumerFactory` + `batchKafkaListenerContainerFactory`

```java
@Bean
public ConsumerFactory<String, Message> batchConsumerFactory(...) {
    Map<String, Object> config = new HashMap<>(kafkaProperties.buildConsumerProperties());
    config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);     // batch 크기 상한
    // ...
}

@Bean
public ConcurrentKafkaListenerContainerFactory<String, Message> batchKafkaListenerContainerFactory(...) {
    var factory = new ConcurrentKafkaListenerContainerFactory<String, Message>();
    factory.setConsumerFactory(batchConsumerFactory);
    factory.setBatchListener(true);                              // ← 핵심 토글
    factory.setCommonErrorHandler(kafkaErrorHandler);
    return factory;
}
```

기존 `consumerFactory`/`kafkaListenerContainerFactory`를 그대로 두고 **두 번째 factory pair**를 만든 이유: 단일 record listener와 batch listener가 같은 토픽을 다르게 소비해야 하므로 설정이 분리되어야 한다.

### 2.2 다른 group-id로 같은 토픽 구독

```java
@KafkaListener(
    topics = Topics.MESSAGES,
    groupId = "springboot-mq-batch-consumer",   // ← MessageConsumer와 다른 group
    containerFactory = "batchKafkaListenerContainerFactory"
)
public void onBatch(List<ConsumerRecord<String, Message>> records) {
    log.info("Batch received: size={}", records.size());
    ...
}
```

- group-id가 다르면 broker는 별개 consumer로 인식 → **같은 record를 두 consumer가 독립적으로 받는다** (D-1에서 정리한 pub/sub 패턴).
- group-id를 `@KafkaListener` 어노테이션에서 override 했다 (yml의 기본값보다 우선).

### 2.3 burst 엔드포인트

```java
@PostMapping("/burst")
public ResponseEntity<Void> burst(@RequestParam(defaultValue = "10") int count) {
    Instant now = Instant.now();
    for (int i = 0; i < count; i++) {
        producer.send(new Message("burst-" + UUID.randomUUID(), "burst#" + i, now));
    }
    return ResponseEntity.accepted().build();
}
```

UUID 키이므로 partition에 골고루 흩어진다 (3 partition에 평균 8.3개씩).

## 3. 관찰 결과

25개 메시지 burst → 두 consumer의 처리 양상:

```
MessageConsumer (single-record):
  25번의 개별 "Received" 로그

BatchMessageConsumer (batch):
  Batch received: size=10
  Batch received: size=10
  Batch received: size=5
```

batch 크기 패턴:
- 10, 10, 5 → MAX_POLL_RECORDS 캡인 10에 닿거나 더 적은 묶음으로 끝남
- 같은 토픽의 정확히 같은 record들을 두 consumer가 처리 (group-id 다름)

총 처리 record 수: 양쪽 모두 25 (분실 / 중복 없음)

## 4. 학습 포인트

### 4.1 batch는 throughput / 다운스트림 효율의 도구

각 record를 외부 API 1회 호출로 처리하면 RTT가 곱해진다. 10 record를 한 batch로 묶어 1회 호출하면 RTT 90% 절감. 단, **batch 안 한 record 처리가 실패하면 어떻게 할지** 결정해야 한다:

| 전략 | 의미 |
|---|---|
| All-or-nothing | 한 record라도 실패하면 batch 전체 retry → 같은 record들이 또 처리됨 (idempotent해야 함) |
| Sub-batch retry | 실패한 record만 분리해서 별도 처리 (복잡) |
| Per-record DLT | 변환 단계 실패는 즉시 DLQ에 모아두고, 송신 성공한 record만 commit |

→ grip-marketing의 `runCatchingBuffer` + `flushDlq` 가 세 번째 전략이다. 학습용으로는 첫 번째(All-or-nothing)가 단순.

### 4.2 `MAX_POLL_RECORDS` 의 의미

한 번의 `consumer.poll()` 호출이 반환할 최대 record 수. 이것이 그대로 batch listener의 `List` 크기 상한이 된다.

- 너무 크게: 한 batch 처리 시간이 길어져 `max.poll.interval.ms` 초과 위험
- 너무 작게: batch 효과 미미
- 다운스트림 API 한계와 정렬 (예: Braze users/track 75건 제한 → 30~50)

### 4.3 invariant 다시 확인

> **batch 처리 시간 + (있다면) 재시도 backoff 총합 < `max.poll.interval.ms`**

batch가 커질수록 처리 시간도 늘어나므로 비율이 위험해진다. 모니터링 포인트.

### 4.4 같은 record를 두 consumer가 보는 게 자연스러운 이유

Kafka는 **broker가 메시지를 보관**한다 (consumer가 가져갔다고 지워지지 않음). consumer group은 단지 "어디까지 읽었는지"를 group 단위로 별도 관리할 뿐. → 새 group이 붙으면 그 group의 offset이 따로 추적되어 같은 record를 자기 페이스로 다시 읽을 수 있다.

이 특성이 RabbitMQ 같은 전통적 큐와의 가장 큰 차이.

## 5. 다음

- D-3: manual offset commit으로 "처리 완료 후에만 commit" 패턴
- 이후 E: Micrometer + Prometheus + Grafana
