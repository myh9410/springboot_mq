# 2026-05-17 — Producer 1차 구현 (시나리오 A-1, String 메시지)

> 1차 draft. 추후 다시 정리할 예정.

## 1. 목표

Producer 한쪽만 먼저 구현해 메시지가 정말 토픽에 들어가는지 시각화한다. 메시지 타입은 `String` (가장 단순). Consumer는 다음 라운드.

## 2. 결정 사항

| 항목 | 선택 | 이유 |
|---|---|---|
| 메시지 타입 | `String` | A-1 단계는 KafkaTemplate 동작 자체에 집중. JSON DTO는 A-2에서 |
| 토픽명 | `messages` | 첫 학습 토픽. 도메인 명사 1개로 단순화. 자동 토픽 생성 활용 |
| 클래스 네이밍 | 접미사 없음 (`MessageProducer`, `MessageController`) | 패키지(`producer/`)가 이미 역할 구분 |
| 직렬화 | `StringSerializer` (key/value 모두) | A-1 단계 단순화 |
| 설정 위치 | `bootstrap-servers` → yml / serializer → Java config | 환경 의존은 yml, 코드 결정은 Java |

## 3. 작업 결과

### 패키지 구조

```
io.github.myh9410.mq/
├── SpringbootMqApplication.java
└── producer/
    ├── KafkaProducerConfig.java   # ProducerFactory + KafkaTemplate 빈 등록
    ├── MessageProducer.java       # KafkaTemplate 래핑, send 메서드
    └── MessageController.java     # POST /messages → producer 호출
```

### application.yml 추가

```yaml
spring:
  application:
    name: springboot-mq
  kafka:
    bootstrap-servers: localhost:9094
```

호스트(맥)에서 컨테이너 내부 Kafka에 붙기 위해 `EXTERNAL` 리스너 포트(9094)를 지정.

### KafkaProducerConfig — 핵심 패턴

```java
@Bean
public ProducerFactory<String, String> producerFactory(KafkaProperties kafkaProperties) {
    Map<String, Object> config = new HashMap<>(kafkaProperties.buildProducerProperties());
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return new DefaultKafkaProducerFactory<>(config);
}
```

- `KafkaProperties.buildProducerProperties()` 가 yml에 적힌 `spring.kafka.*` 값을 모두 읽어 `Map<String, Object>` 로 만들어준다.
- 그 위에 Java 코드로 serializer만 덮어쓰는 패턴 — **환경 의존(yml)과 코드 결정(Java)이 자연스럽게 분리**된다.
- `@ConditionalOnMissingBean` 덕에 내가 직접 `ProducerFactory`/`KafkaTemplate` 빈을 만들면 Boot 자동설정은 알아서 백오프한다.

## 4. 학습 포인트

### 4.1 Boot 4.0의 `KafkaProperties` import 경로 변경

Boot 3.x: `org.springframework.boot.autoconfigure.kafka.KafkaProperties`
Boot 4.0: `org.springframework.boot.kafka.autoconfigure.KafkaProperties`

Boot 4.0에서 Kafka 모듈이 별도 module(`spring-boot-kafka-autoconfigure`)로 분리되면서 패키지가 평면화됐다. 옛 자료를 그대로 따라하면 컴파일 안 됨.

### 4.2 `kafkaTemplate.send(...)` 반환 타입은 `CompletableFuture`

옛 자료에는 `ListenableFuture` 가 자주 보이는데, **Spring Kafka 3.0+ 부터 `CompletableFuture` 로 통일**됐다. 그래서 콜백은 `whenComplete((result, ex) -> ...)` 패턴.

```java
kafkaTemplate.send(TOPIC, payload)
    .whenComplete((result, ex) -> {
        if (ex == null) {
            var meta = result.getRecordMetadata();
            log.info("Sent ok: topic={} partition={} offset={}", ...);
        }
    });
```

`send()` 자체는 **비동기 (non-blocking)**. HTTP 응답을 먼저 돌려보내고 실제 송신/응답은 별도 producer 스레드(`t-mq-producer-1`)에서 진행된다. 로그에 보이는 스레드명 차이로 확인 가능.

### 4.3 자동 토픽 생성

producer가 존재하지 않는 토픽으로 send 하면, `auto.create.topics.enable=true` 인 브로커는 그 자리에서 토픽을 생성한다. 학습 환경에서 편리하지만 운영에서는 보통 비활성화하고 사전 생성한다 (오타로 인한 토픽 난립 방지).

### 4.4 partition / offset의 의미를 첫 메시지로 체감

첫 send 결과 로그: `partition=0 offset=0`.
- 단일 브로커 + 신규 토픽이라 partition 1개(기본값)
- 메시지가 처음이라 offset 0
- 같은 토픽에 또 보내면 offset은 1, 2, ... 로 증가

## 5. 운영 / 환경 이슈

### 5.1 `bitnami/kafka:3.7` 이미지 사라짐

이전 단계에서 docker-compose에 적어둔 `bitnami/kafka:3.7` 이미지가 Docker Hub에서 pull 실패. Bitnami가 2025년 이후 이미지 배포 정책을 바꿔 옛 태그를 legacy registry로 이전.

→ **공식 `apache/kafka:latest` 이미지로 전환.** 환경변수 컨벤션이 다르다:

| | Bitnami | Apache (공식) |
|---|---|---|
| 환경변수 prefix | `KAFKA_CFG_*` | `KAFKA_*` |
| 데이터 디렉토리 | `/bitnami/kafka` | `/var/lib/kafka/data` |
| 자동 PLAINTEXT 허용 | `ALLOW_PLAINTEXT_LISTENER=yes` 필요 | 별도 플래그 없음 |

리스너 구성과 KRaft 모드 설정 자체는 동일.

### 5.2 포트 8080 충돌

Kafka UI 컨테이너가 8080을 점유 중인 상태로 Spring Boot 띄우니 `Web server failed to start. Port 8080 was already in use` 에러.

→ **Boot의 8080은 관례라 유지**하고 Kafka UI를 호스트 측 `8090` 으로 옮김 (컨테이너 내부는 그대로 8080). 다른 옵션은 Spring 쪽 `server.port` 를 바꾸는 것이지만, 학습 자료/관례와의 일치를 위해 앱이 우선권을 갖도록 함.

## 6. End-to-End 검증 절차

```bash
# 1) Kafka + UI 띄우기
cd docker && docker compose up -d

# 2) 앱 실행
./gradlew bootRun

# 3) 메시지 발사
curl -X POST http://localhost:8080/messages \
  -H "Content-Type: text/plain" \
  -d "hello kafka from springboot_mq"
# -> HTTP/1.1 202

# 4) 앱 로그에서 확인
# Sending: topic=messages payload=hello kafka from springboot_mq
# Sent ok: topic=messages partition=0 offset=0

# 5) 토픽에 실제로 들어갔는지 확인
docker exec springboot-mq-kafka /opt/kafka/bin/kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 --topic messages \
  --from-beginning --max-messages 1 --timeout-ms 5000
# -> hello kafka from springboot_mq

# 6) Kafka UI에서도 확인
# http://localhost:8090 -> Topics -> messages -> Messages
```

## 7. 남은 TODO

- [ ] Consumer 측 구현 (`consumer/KafkaConsumerConfig`, `MessageConsumer`)
- [ ] A-2: 메시지를 JSON DTO로 업그레이드 (`JsonSerializer`, 타입 정보 헤더)
- [ ] 키 기반 파티셔닝 실험 (같은 key는 같은 partition으로)
- [ ] Producer 신뢰성 옵션 (`acks=all`, `enable.idempotence`) 실험은 시나리오 B