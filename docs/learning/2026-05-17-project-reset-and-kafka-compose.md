# 2026-05-17 — Kafka docker-compose 구성

> 1차 draft. 추후 다시 정리할 예정.

## 1. 배경

springboot_mq를 학습용 샌드박스로 재출발하면서, Kafka를 OrbStack 기반 로컬 컨테이너로 띄울 수 있는 환경을 가장 먼저 구성한다.

## 2. 의사결정 — Kafka 컨테이너 구성

### 질문

> orbstack을 통해서 kafka 설정된 컨테이너를 띄울 수 있는 docker compose 파일을 만들고 싶어.

### 옵션

- A. KRaft 단일 브로커 + Kafka UI
- B. KRaft 단일 브로커만
- C. Zookeeper + Kafka + Kafka UI (옛 방식)

### 결정: **A**

### 왜 이게 적절한가 (학습 포인트)

- **KRaft**: Kafka 2.8부터 도입되어 3.x에서 정식화된 모드. Zookeeper 없이 Kafka 자체가 메타데이터 관리(컨트롤러 쿼럼)를 수행. Kafka 4.x부터는 Zookeeper 지원이 제거될 예정이라 새로 만드는 환경에서 굳이 ZK를 두는 건 비추.
- **단일 브로커**: 학습 환경이라 클러스터링/리플리케이션이 불필요. `offsets.topic.replication.factor`, `transaction.state.log.replication.factor`, `min.isr`를 모두 1로 둬야 단일 브로커에서 정상 동작.
- **Kafka UI (provectuslabs)**: 토픽/컨슈머/메시지 페이로드를 GUI로 확인. 학습 중에 "내가 보낸 메시지가 정말 들어갔나"를 빠르게 확인하기 좋음. http://localhost:8080.

### 리스너 설계 (가장 헷갈리는 부분)

Kafka의 리스너는 "어디서 들어오는 연결을 받을 것인가"와 "클라이언트에게 어떤 주소로 다시 접속하라고 알려줄 것인가(advertised)"가 분리되어 있다. 단일 브로커여도 **호스트(맥 OS)에서 접속**하는 케이스와 **컨테이너 네트워크 내부**에서 접속하는 케이스 양쪽을 다 만족시키려면 리스너를 두 개 두는 게 표준이다.

| 리스너 이름 | 포트 | advertised | 용도 |
|---|---|---|---|
| `PLAINTEXT` | 9092 | `kafka:9092` | 같은 docker network 안의 컨테이너 (예: Kafka UI) |
| `CONTROLLER` | 9093 | — | KRaft 컨트롤러 쿼럼 내부 통신 |
| `EXTERNAL` | 9094 | `localhost:9094` | 호스트(맥)에서 Spring Boot 앱이 접속 |

→ Spring Boot 쪽 `spring.kafka.bootstrap-servers`는 `localhost:9094`로 설정하면 된다.

만약 둘을 한 리스너로 합치면 advertised가 한쪽 기준이 되어버려서, 컨테이너에서는 되는데 호스트에선 안 되거나 그 반대가 발생한다. 흔한 함정.

## 3. 남은 TODO / 다음에 생각해볼 거리

- [ ] `docker compose up -d` 실제로 띄워서 Kafka UI에 브로커가 보이는지 확인
- [ ] Spring Boot 프로젝트를 다시 부트스트랩할지, 라이브러리/모듈 단위로 처음부터 쌓을지 결정
- [ ] Producer/Consumer를 만들 때 어떤 학습 목표를 두고 시작할지 (예: at-least-once 의미론, idempotent producer, consumer group rebalancing 관찰 등)
- [ ] 이 노트를 추후 "Kafka 로컬 개발환경 가이드" 형태로 정리 (1차 draft → 정돈된 reference 문서로)