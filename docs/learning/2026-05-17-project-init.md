# 2026-05-17 — 프로젝트 초기화 (언어/빌드/Boot 버전 결정)

> 1차 draft. 추후 다시 정리할 예정.

## 1. 배경

리셋된 `main` 브랜치 위에서 Spring Boot 프로젝트를 새로 부트스트랩한다. Kafka 학습이 1차 목표지만, 그 전에 언어/빌드 도구/JDK/Boot 버전을 결정해야 한다.

## 2. 결정 사항

| 항목 | 선택 | 비고 |
|---|---|---|
| 언어 | Java | 명시적 boilerplate가 학습에 도움 |
| 빌드 도구 | Gradle (Groovy DSL) | 자료가 많고 옛 프로젝트 컨벤션과 일치 |
| JDK | 25 (LTS) | 현 시점 최신 LTS, virtual thread 등 신기능 학습 가능 |
| Spring Boot | 4.0.6 | 최신 GA 안정판. starter 구조 변경점도 함께 익히는 게 가치 있음 |
| Gradle Wrapper | 9.4.1 | Initializr가 Boot 4.0 기본값으로 제공 |
| 설정 파일 | YAML | 중첩 키 가독성이 좋아 Kafka 설정 추가 시 유리 |

## 3. 패키지명 결정

### 컨벤션

Java 패키지는 **역도메인 방식**이 표준이다. 본인이 소유한/연관된 namespace를 역순으로 쓴다.

| 형태 | 언제 쓰나 |
|---|---|
| `com.<회사>.<제품>` | 회사 도메인 소유 (실무 표준) |
| `dev.<유저>.<프로젝트>` | 개인 작업, 도메인 없을 때 |
| `io.github.<유저>.<프로젝트>` | GitHub에 공개 배포 의도가 있을 때 |
| `com.example.<프로젝트>` | Initializr 기본값, 학습용 placeholder |

### 결정: `io.github.myh9410.mq`

GitHub(`myh9410/springboot_mq`)에 올라가 있고 신원이 명확하다. 옛 코드는 `com.springboot.mq`였는데, `com.springboot.*`는 사실상 Spring 프레임워크 자체가 점유할 만한 namespace라 회피.

## 4. Spring Boot 4.0 변경점 (이번에 처음 마주친 부분)

Boot 4.0은 starter 컨벤션을 통일하면서 몇 가지 이름이 바뀌었다.

| Boot 3.x | Boot 4.0 |
|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `org.springframework.kafka:spring-kafka` (라이브러리 직접) | `spring-boot-starter-kafka` (Boot 신규 starter) |
| `spring-boot-starter-test` (통합) | `spring-boot-starter-webmvc-test`, `-kafka-test`, `-actuator-test` 등 분리 |

### `spring-kafka` vs `spring-boot-starter-kafka` — 학습 포인트

이 둘의 관계가 헷갈리기 쉽다. 정리:

- **`org.springframework.kafka:spring-kafka`**
  - Spring 팀이 만든 **실제 Kafka 통합 라이브러리**. `KafkaTemplate`, `@KafkaListener`, `ConcurrentKafkaListenerContainerFactory`, `DefaultKafkaProducerFactory` 같은 핵심 클래스가 들어있다.
  - `org.apache.kafka:kafka-clients`도 transitive로 끌고 온다.
- **`org.springframework.boot:spring-boot-starter-kafka`**
  - Spring Boot 팀이 만든 **starter**. Boot 4.0에서 새로 도입됐다.
  - 내부적으로는 `spring-kafka`를 그대로 끌어오는 얇은 wrapper.

#### Boot 3.x에서는 왜 starter가 없었나

3.x 시절엔 Kafka용 starter가 존재하지 않았고, 관례는 그냥 `spring-kafka`를 직접 의존성으로 추가하는 것이었다. 자동설정(`KafkaAutoConfiguration`)은 `spring-boot-autoconfigure`에 들어있어, classpath에 `spring-kafka`만 있으면 자동으로 트리거됐다 (`spring-boot-autoconfigure`는 다른 starter들이 transitive로 가져옴).

Boot 4.0에서 비로소 다른 모듈과 컨벤션을 통일하기 위해 starter로 승격됐다.

## 5. 한 일

1. Spring Initializr API로 `starter.zip` 다운로드
   - 처음: Boot 3.x 최신(3.5.14)로 받음 → 사내 표준과 격차가 큼을 확인 후 재결정
   - 최종: Boot 4.0.6 + JDK 25로 재다운로드
2. 프로젝트 루트에 압축 해제, 기존 `.gitignore`는 Initializr 버전과 병합
3. `application.properties` → `application.yml` 변환
4. `HELP.md` 제거 (Initializr placeholder)
5. `./gradlew compileJava`로 빌드 검증 → BUILD SUCCESSFUL

## 6. 결과 디렉토리 구조

```
springboot_mq/
├── build.gradle                # Boot 4.0.6 + JDK 25 toolchain
├── settings.gradle
├── gradlew, gradlew.bat
├── gradle/wrapper/
├── src/main/
│   ├── java/io/github/myh9410/mq/SpringbootMqApplication.java
│   └── resources/application.yml
├── src/test/java/io/github/myh9410/mq/SpringbootMqApplicationTests.java
├── docker/docker-compose.yml   # 이전 단계에서 만든 Kafka 환경
├── docs/learning/              # 학습 노트
└── README.md
```

## 7. 남은 TODO / 다음에 생각해볼 거리

- [ ] `application.yml`에 Kafka `bootstrap-servers: localhost:9094` 추가
- [ ] 첫 Producer/Consumer + REST Controller 구현 (시나리오 A)
- [ ] `docker compose up -d` → 메시지 송수신 → Kafka UI에서 확인
- [ ] Boot 4.0 기준의 `KafkaProperties`, `MessageConverter` 경로 변경점 학습 (Boot 3.x 기준 자료와 다른 부분)