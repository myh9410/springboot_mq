# 2026-06-03 — D-3: Manual Offset Commit

> 1차 draft. 추후 다시 정리할 예정.

## 1. 목표

"메시지를 받은 직후"가 아니라 "처리 완료 직후"에만 broker에 offset을 commit하도록 컨트롤. 처리 도중에 죽으면 다음 instance가 같은 record를 다시 받는다 — **at-least-once 의미론**을 컨슈머 측에서 직접 강제.

## 2. AckMode 종류 (Spring Kafka)

| 모드 | 동작 | 언제 |
|---|---|---|
| `RECORD` | 리스너 메서드 return 즉시 자동 commit | 한 건씩 안전 처리 |
| `BATCH` (기본값) | 한 batch 처리 완료 후 commit | 일반적 자동 모드 |
| `MANUAL` | 리스너가 `ack.acknowledge()` 호출, 다음 poll 직전에 commit | 비즈 완료 시점 명시 |
| **`MANUAL_IMMEDIATE`** | `ack.acknowledge()` 직후 즉시 commit | 즉시성 보장 (이번 라운드 선택) |

> **참고**: AckMode를 설정하면 Spring Kafka가 자동으로 consumer 클라이언트의 `enable.auto.commit=false`로 둔다. 우리가 따로 yml에서 설정할 필요 없다.

## 3. 코드 핵심

### Container Factory

```java
factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
```

### Listener — `Acknowledgment` 파라미터 추가

```java
@KafkaListener(
    topics = Topics.MESSAGES,
    groupId = "springboot-mq-manualack-consumer",
    containerFactory = "manualAckKafkaListenerContainerFactory"
)
public void onMessage(ConsumerRecord<String, Message> record, Acknowledgment ack) {
    if (msg.content().startsWith("noack")) {
        log.warn("Skipping ack ...");
        return;                  // ack 호출 안 함 → offset 미커밋
    }
    log.info("Processed ...");
    ack.acknowledge();           // 명시적 commit
}
```

## 4. 검증 시나리오 ✅

### Step 1 — 정상 + 'noack' 메시지 송신

```
POST /messages {"id":"manual-001","content":"normal-payload",...}
POST /messages {"id":"manual-002","content":"noack-this",...}
```

### Step 2 — 컨슈머 로그

```
ManualAck processed:     partition=1 offset=11 key=manual-001
ManualAck SKIPPING ack:  partition=2 offset=16 key=manual-002
```

### Step 3 — 컨슈머 그룹 commit 상태 확인

```bash
$ kafka-consumer-groups.sh --describe --group springboot-mq-manualack-consumer
GROUP                            TOPIC     PARTITION  CURRENT-OFFSET  LOG-END-OFFSET  LAG
springboot-mq-manualack-consumer messages  0          9               9               0
springboot-mq-manualack-consumer messages  1          12              12              0
springboot-mq-manualack-consumer messages  2          16              17              1
```

→ **partition=2 LAG=1** ← ack 안 한 manual-002가 정확히 1개 남아있다는 증거.

### Step 4 — 앱 재시작 직후 로그

```
ManualAck SKIPPING ack: partition=2 offset=16 key=manual-002 (redelivered on restart)
```

같은 offset 16의 record가 다시 도착. **at-least-once 의미가 행동으로 입증됨.**

## 5. 학습 포인트

### 5.1 "AT-LEAST-ONCE" 의 미묘함

Producer 측에서 idempotent producer (B-1) 켰어도, 컨슈머 측은 처리/ack 사이의 죽음으로 **재처리 가능**. 즉:

- Producer → broker: at-most-once or exactly-once (선택 가능)
- broker → Consumer 처리: **at-least-once가 기본**

진짜 exactly-once-end-to-end는 (producer transactional) + (consumer read-process-write transaction) 조합으로만 가능. **그래서 운영에서 가장 자주 만드는 안전망은 "비즈 로직을 idempotent하게 만들기"** 다 (같은 record를 두 번 처리해도 결과가 같도록).

### 5.2 LAG = 0이 항상 좋은 건 아니다

LAG = "broker에 쌓인 다음 record offset" - "consumer가 commit한 마지막 offset". `LAG = 0` 은 처리가 따라잡혔다는 의미.

하지만 manual ack에서 일부러 ack 안 한 상황에서도 LAG이 1로 머무는 것처럼, **lag이 줄지 않는다 = 처리가 막혔거나 ack를 안 하고 있다**. 운영에서는 **lag의 시간적 추세**가 더 중요한 지표.

### 5.3 stuck consumer 문제

위 시나리오의 결말이 시사하는 바: ack를 영원히 안 호출하면, 컨슈머는 같은 record를 무한히 다시 받는다 (`max.poll.interval.ms` 안에 처리하기만 하면 rebalance도 안 됨, 그냥 영원히 같은 자리 반복).

운영에서 stuck consumer 감지: **commit offset 진행이 정지 + LAG 증가**. Micrometer로 보면 즉시 알람 가능.

→ 해결책은 보통 두 가지:
1. **Skip + DLT**: 일정 횟수 retry 후 못 처리하는 record는 DLQ로 보내고 ack해서 다음으로 진행 (C-1+C-2 패턴)
2. **수동 개입**: 운영자가 직접 offset을 forward seek

### 5.4 MANUAL vs MANUAL_IMMEDIATE 차이

| | MANUAL | MANUAL_IMMEDIATE |
|---|---|---|
| commit 시점 | 다음 poll 직전 (배치) | ack 호출 직후 (즉시) |
| 효율 | 더 좋음 (commit 호출 횟수 적음) | 약간 손해 |
| 즉시성 | ack 후에도 잠시 미커밋 | 호출 즉시 |
| 운영 안전성 | 같은 poll 안 다른 record 미처리 시 같이 영향 | 더 안전 |

학습용에는 즉시성이 명확한 IMMEDIATE를 선택.

## 6. 현재 시스템에 추가된 4번째 listener container 정리

이번 라운드까지 동작 중인 listener container factory 4개:

| 빈 이름 | 용도 | mode |
|---|---|---|
| `kafkaListenerContainerFactory` | 단일 record 기본 (MessageConsumer) | auto-ack (BATCH) |
| `stringKafkaListenerContainerFactory` | DLQ 관찰 (DlqConsumer) | auto-ack |
| `batchKafkaListenerContainerFactory` | batch 처리 (BatchMessageConsumer) | auto-ack |
| `manualAckKafkaListenerContainerFactory` | 수동 commit (ManualAckMessageConsumer) | MANUAL_IMMEDIATE |

→ 모두 같은 `messages` 토픽을 다른 group-id로 구독해 독립적으로 처리. 학습용 한 컨테이너에 패턴 다양성을 모은 셈.

## 7. 다음

- E: Micrometer + Prometheus + Grafana 로 lag / throughput 시각화
