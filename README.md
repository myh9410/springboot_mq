# springboot_mq

Spring Boot + Kafka 학습용 샌드박스 프로젝트.

## 프로젝트 스펙

| 항목 | 값 |
|---|---|
| 언어 | Java 25 |
| 프레임워크 | Spring Boot 4.0.6 |
| 빌드 도구 | Gradle 9.4.1 (Groovy DSL) |
| 패키지 베이스 | `io.github.myh9410.mq` |
| group / artifact | `io.github.myh9410` / `springboot-mq` |
| 설정 파일 형식 | YAML (`application.yml`) |
| 로컬 인프라 | Kafka (KRaft) + Kafka UI via Docker Compose (OrbStack) |

### 의존성

- `spring-boot-starter-webmvc` — REST 엔드포인트
- `spring-boot-starter-kafka` — Spring for Apache Kafka
- `spring-boot-starter-actuator` — health / metrics
- 테스트: `spring-boot-starter-webmvc-test`, `spring-boot-starter-kafka-test`, `spring-boot-starter-actuator-test`

## 개요

- 목적: 메시지 큐(우선 Kafka) 관련 패턴을 실험하고 학습한다.
- 이전 코드 이력은 `archive/legacy` 브랜치에 보존되어 있다 (잔디 유지 목적).
- 현재 `main` 브랜치는 새 시작점이며, 이전 히스토리와는 분리되어 있다.

## 디렉토리 구조

```
.
├── build.gradle              # Gradle 빌드 스크립트
├── settings.gradle
├── gradlew, gradlew.bat      # Gradle wrapper
├── gradle/wrapper/
├── src/
│   ├── main/
│   │   ├── java/io/github/myh9410/mq/    # 애플리케이션 코드
│   │   └── resources/application.yml      # Spring 설정
│   └── test/java/io/github/myh9410/mq/
├── docker/                   # 로컬 개발용 인프라 (Kafka, Kafka UI)
│   └── docker-compose.yml
├── docs/learning/            # 학습 노트와 의사결정 로그
└── README.md
```

## 빌드 및 실행

JDK 25가 설치되어 있어야 한다 (Gradle toolchain이 자동 선택).

```bash
./gradlew build              # 컴파일 + 테스트
./gradlew bootRun            # 애플리케이션 실행
```

## 로컬 Kafka 띄우기

OrbStack에서 Docker가 실행 중이어야 한다.

```bash
cd docker
docker compose up -d
```

| 서비스 | 호스트 접근 | 컨테이너 내부 접근 |
|---|---|---|
| Kafka 브로커 | `localhost:9094` | `kafka:9092` |
| Kafka UI | http://localhost:8090 | — |
| Prometheus | http://localhost:9090 | `prometheus:9090` |
| Grafana | http://localhost:3000 (admin / admin) | — |

KRaft 모드(단일 브로커, Zookeeper 없음)로 동작한다. 인증은 없는 PLAINTEXT 구성이며, **로컬 학습 외 목적으로 사용하지 말 것**.

### 종료

```bash
docker compose down            # 컨테이너만 정리
docker compose down -v         # 볼륨(데이터)까지 삭제
```

## 학습 로그

의사결정 흐름과 질문/답변은 `docs/learning/` 아래에 정리한다. 1차로 draft를 남기고 추후 정리하는 흐름을 따른다.