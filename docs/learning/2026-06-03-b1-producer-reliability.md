# 2026-06-03 — B-1: Idempotent Producer & 신뢰성 옵션

> 1차 draft. 추후 다시 정리할 예정.

## 1. 목표

producer 측 메시지 분실/중복/순서 깨짐을 막는 표준 옵션 4가지를 켜고, **"exactly-once"** 라는 단어가 실제로 가리키는 좁은 의미를 정리한다.

## 2. 추가된 4개 옵션

```java
config.put(ProducerConfig.ACKS_CONFIG, "all");
config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 3);
config.put(ProducerConfig.RETRIES_CONFIG, 5);
```

### 2.1 `acks = "all"` (= `-1`)

브로커가 ack를 언제 보내줄지의 정책.

| 값 | ack 시점 | 위험 |
|---|---|---|
| `0` | producer가 send 직후 ack 가정 | broker에 도달 안 해도 모름. 가장 빠르고 가장 위험 |
| `1` | leader replica가 자기 로그에 기록한 직후 | leader가 follower 복제 전에 죽으면 메시지 손실 |
| `all` (`-1`) | leader + 모든 in-sync replica가 기록한 뒤 | 분실 최소화. 약간 느려짐 |

단일 브로커 환경(우리)에서는 `all`이라도 leader 하나뿐이라 실효는 없다. 운영 멀티 브로커에선 의미가 크다. `min.insync.replicas` 와 함께 동작.

### 2.2 `enable.idempotence = true`

producer가 메시지마다 **sequence number + producer id** 를 부여한다. 같은 메시지가 네트워크 재시도로 두 번 도착해도 브로커가 sequence number로 dedup → **partition 내부에 정확히 1번만 기록**된다.

로그에서 보이는 활성화 증거:
```
TransactionManager : [Producer clientId=...] ProducerId set to 1009 with epoch 0
```

### 2.3 `max.in.flight.requests.per.connection = 3`

producer가 ack 받기 전에 동시에 띄울 수 있는 in-flight 요청 수. **idempotence 켤 때는 5 이하**여야 한다 (Kafka 3+). 그 이상이면 broker가 sequence number 추적을 못 함.

값을 줄이면 (예: 1) **partition 내 순서가 더 엄격하게** 보장되지만 throughput 손해. 3은 throughput과 순서 보장의 일반적 절충값.

### 2.4 `retries = 5`

일시적 네트워크 에러(예: `NotEnoughReplicasException`, leader 전환 중)에 producer가 알아서 재시도하는 횟수. `enable.idempotence=true`와 함께면 재시도해도 중복이 안 생기므로 안전하게 늘려도 됨.

## 3. "Exactly-once" 라는 단어의 좁은 의미

idempotent producer는 다음 케이스만 처리한다:

> **같은 producer가 같은 메시지를 같은 partition에 두 번 이상 보내려고 할 때, 브로커가 sequence number로 dedup해서 한 번만 기록한다.**

다음은 보장 안 함:

| 케이스 | idempotent producer로 안 되는 이유 |
|---|---|
| 다른 producer 인스턴스가 같은 메시지를 또 보냄 | producer id가 다름 → 다른 메시지로 본다 |
| consumer 측 중복 처리 (acked 직전 죽음) | producer 영역 밖. consumer가 처리 → ack 사이에서 죽으면 다음 instance가 또 처리 |
| 여러 partition / 여러 토픽 걸친 atomic 발행 | 그건 `transactional producer` (`transactional.id`) 영역 |

→ **end-to-end exactly-once는 producer idempotence + transactional producer + consumer side dedup or read-process-write transaction** 의 조합이다. idempotent producer 하나만으론 "정확히 한 번"이 아니라 "send 단계에서 중복 안 생김"이 정확한 표현.

## 4. 단일 브로커에서 acks=all로도 손실 가능성

`acks=all` + `min.insync.replicas=1` + 브로커 1대 → 그 브로커가 죽으면 leader도 follower도 없어서 메시지는 **acked 됐어도 영구 손실 가능**. 진짜 안전은:

- 브로커 3대 이상
- replication factor ≥ 3
- `min.insync.replicas ≥ 2`
- `acks=all`

이 4가지가 한 세트. 학습 환경에서는 흉내만 낸 셈.

## 5. 검증 결과

```
[Producer config in startup log]
acks = -1
enable.idempotence = true
max.in.flight.requests.per.connection = 3
retries = 5 (기본보다 큰 값으로 반영됨)

[First send activation]
TransactionManager: ProducerId set to 1009 with epoch 0
```

`epoch` 는 producer 재시작 횟수 카운터로, fenced-out (좀비 producer 차단) 메커니즘에 쓰인다.

## 6. 다음

- B-2: `ProducerListener` 로 모든 send 결과를 한 곳에서 처리 (현재는 `MessageProducer` 안의 `whenComplete` 으로 분산되어 있음)
- 향후 transactional producer 실험은 시나리오 외
