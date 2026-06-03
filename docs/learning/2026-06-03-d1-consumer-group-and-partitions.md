# 2026-06-03 — D-1: Consumer Group과 partition 분배

> 1차 draft. 추후 다시 정리할 예정.

## 1. 목표

지금까지 partition 1개로만 동작시켜서 못 봤던 Kafka의 핵심 동작 — **partition 단위로 일이 분산되고 같은 key는 같은 partition으로 묶인다** — 를 직접 관찰한다.

## 2. 셋업 변경

| 항목 | 값 | 의미 |
|---|---|---|
| `messages` partition 수 | 3 | `NewTopic` 빈으로 명시 |
| `messages.dlq` partition 수 | 1 | DLQ는 보관 / 재발행 용도라 보통 1개로 충분 |
| listener factory `concurrency` | 3 | 같은 JVM 안에서 3개 consumer thread를 띄움 |

```java
// TopicConfig.java
@Bean
NewTopic messagesTopic() {
    return TopicBuilder.name(Topics.MESSAGES).partitions(3).replicas(1).build();
}
```

```java
// KafkaConsumerConfig.java — kafkaListenerContainerFactory()
factory.setConcurrency(3);
```

> **주의**: `NewTopic` 빈은 토픽이 부재할 때만 생성한다. 기존 토픽이 이미 1 partition으로 있으면 partition 수를 자동으로 늘리지 않는다. 기존 토픽을 한 번 삭제하고 시작해야 한다.

## 3. 관찰 결과 — partition 분배

키 9개(`a` ~ `i`)로 동시에 메시지를 보낸 결과:

```
ntainer#1-0-C-1 → partition=0 (keys: f, g)
ntainer#1-1-C-1 → partition=1 (keys: a, c, h)
ntainer#1-2-C-1 → partition=2 (keys: b, d, e, i)
```

읽을 수 있는 사실들:

1. **각 thread가 partition 1개씩 차지**. concurrency=3 + partitions=3 → 깔끔하게 1대1.
2. **같은 key는 항상 같은 partition**. `a`는 늘 partition=1, `e`는 늘 partition=2. (key의 hash % partition 수로 결정)
3. **분배는 균등하지 않다**. 3개 키씩 정확히 나뉘는 게 아니라 hash 분포에 따라 2/3/4개씩 분포. 키 공간이 크면 평균적으로 균등해진다.
4. **순서 보장 범위가 partition 내부**. 같은 key 메시지들은 같은 thread에서 producer가 보낸 순서대로 처리된다 — 다른 key와의 순서는 보장 안 됨.

### Thread 이름 규칙: `ntainer#X-Y-C-1`

- `X` : 어느 ContainerFactory에서 만들어진 컨테이너인지 (우리는 main listener factory 1번, string factory 0번)
- `Y` : 그 컨테이너 안의 consumer index (concurrency=3이면 0, 1, 2)
- `C-1` : Kafka consumer 표준 suffix

이 로그 패턴 하나로 "어느 partition을 누가 처리하고 있는가"가 즉시 드러난다.

## 4. 학습 포인트

### 4.1 partition 수가 곧 병렬 처리 상한

같은 group-id로 consumer를 더 띄워도, **partition 수보다 많은 consumer는 idle 상태**가 된다. 4번째 consumer가 들어오면 partition을 할당받지 못함.

→ "처리량을 늘리고 싶으면 partition 수부터 늘려야 한다" 는 격언이 여기서 나온다. 운영에서 partition 수는 신중하게 결정 (사후 증가는 가능하지만 key→partition 매핑이 깨져 순서 보장이 무너진다).

### 4.2 같은 group-id vs 다른 group-id

같은 group-id를 가진 consumer들끼리는 **하나의 작업을 나눠 갖는다** (workload sharing). 한 메시지는 그 group 안의 한 consumer에게만 간다.

다른 group-id로 붙은 두 consumer는 **각자 독립적으로 모든 메시지를 받는다** (pub/sub). 한 메시지가 양쪽에 다 도달.

→ 이번 라운드의 `DlqConsumer`는 group-id가 `springboot-mq-dlq-watcher`로 다르기 때문에 DLQ를 메인 listener와 무관하게 자기만의 페이스로 읽는다.

### 4.3 rebalance가 일어나는 시점

같은 group 안에서 다음 일이 생기면 partition 재배정(rebalance)이 일어난다:

- 새 consumer가 join
- 기존 consumer가 떠남 (정상 종료 or session timeout)
- partition 수가 증가
- 토픽 메타데이터 변경

rebalance 중에는 **그 group의 모든 consumer가 잠깐 처리를 멈춘다** (Stop-the-world 같은 효과). 그래서 운영에서 컨슈머를 자주 띄웠다 내렸다 하는 패턴(짧은 lifecycle pod 등)은 lag 누적의 원인이 된다.

### 4.4 partition key 설계가 도메인 로직과 직결된다

partition key를 어떻게 정하느냐에 따라 다음이 결정된다:

- **순서 보장 단위**: "같은 user_id의 이벤트는 producer 발행 순서대로 처리되어야 한다"는 요구사항은 user_id를 key로 두면 만족
- **부하 균형**: 핫 key가 있으면 (예: 특정 user가 엄청난 트래픽) 그 partition만 과부하 — 부하가 한 consumer에 쏠림
- **확장성**: key 공간이 작으면 partition 수를 늘려도 의미 없음 (key가 적으니 결국 같은 partition으로 모임)

이번에 key를 `message.id` (사실상 UUID-ish)로 정한 건 의미가 없는 분배 — 단지 골고루 흩어지게 하는 용도. 실무에서는 도메인 키(user_id, order_id 등)가 자연스럽다.

## 5. 검증한 시나리오

- ✅ partition 수 == concurrency: 각 thread가 정확히 1 partition 점유
- ✅ key 기반 일관 라우팅: 같은 key는 항상 같은 partition으로

## 6. 더 깊이 들어가려면 (TODO)

- [ ] concurrency > partition 수: 일부 thread가 idle 상태로 머무는 것 관찰
- [ ] concurrency < partition 수: 한 thread가 여러 partition을 처리
- [ ] 별도 JVM으로 2번째 consumer 띄워서 rebalance 로그 관찰
- [ ] partition 수를 증가시켜본 뒤 같은 key가 다른 partition으로 가는 현상 (순서 보장 깨짐)
- [ ] `StickyAssignor` vs `CooperativeStickyAssignor` 차이
