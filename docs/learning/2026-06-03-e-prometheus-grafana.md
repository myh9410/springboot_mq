# 2026-06-03 — E: Micrometer + Prometheus + Grafana

> 1차 draft. 추후 다시 정리할 예정.

## 1. 목표

지금까지 만든 producer / consumer / DLQ / batch / manual-ack listener의 동작을 **숫자로** 측정할 수 있게 한다. throughput, error rate, consumer lag, retry 횟수 같은 운영 지표를 메트릭 시계열로 수집하고 시각화한다.

## 2. 스택

```
Spring Boot App  ── /actuator/prometheus ──> Prometheus (scrape 5s) ──> Grafana
   ↑
Micrometer Listener (producer / consumer 별 client_id로 태그)
```

| 컴포넌트 | 위치 | 포트 |
|---|---|---|
| Micrometer + Prometheus registry | Boot 앱 안 | — |
| `/actuator/prometheus` 엔드포인트 | Boot 앱 안 | 8080 |
| Prometheus | Docker | 9090 |
| Grafana | Docker | 3000 (admin/admin, anonymous Admin 허용) |

## 3. 추가/변경된 부분

### 3.1 의존성

```groovy
runtimeOnly 'io.micrometer:micrometer-registry-prometheus'
```

- `runtimeOnly`: 컴파일 시점엔 필요 없음, 런타임에 classpath만 있으면 PrometheusMeterRegistry가 auto-config됨.

### 3.2 application.yml — 엔드포인트 노출

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus
```

기본은 `health`만 노출. `prometheus`를 화이트리스트에 추가.

### 3.3 Micrometer 리스너 부착

**producer 측**:
```java
DefaultKafkaProducerFactory<String, Object> factory = new DefaultKafkaProducerFactory<>(...);
factory.addListener(new MicrometerProducerListener<>(meterRegistry));
```

**consumer 측 (각 ConsumerFactory마다)**:
```java
DefaultKafkaConsumerFactory<String, Message> factory = new DefaultKafkaConsumerFactory<>(...);
factory.addListener(new MicrometerConsumerListener<>(meterRegistry));
```

→ 결과 메트릭이 **client_id 단위로 태그**되어 producer/consumer를 구분할 수 있다:

```
kafka_consumer_records_consumed_total{client_id="consumer-springboot-mq-batch-consumer-5",spring_id="batchConsumerFactory.consumer-springboot-mq-batch-consumer-5"}
kafka_consumer_records_consumed_total{client_id="consumer-springboot-mq-manualack-consumer-1",spring_id="consumerFactory.consumer-springboot-mq-manualack-consumer-1"}
```

`spring_id` 라벨이 factory 이름까지 보여줘서 "어느 ContainerFactory가 만든 consumer인지" 식별 가능.

### 3.4 docker-compose에 Prometheus + Grafana 추가

```yaml
prometheus:
  image: prom/prometheus:latest
  ports: ["9090:9090"]
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml:ro
  extra_hosts:
    - "host.docker.internal:host-gateway"   # Linux 호환

grafana:
  image: grafana/grafana:latest
  ports: ["3000:3000"]
  environment:
    GF_AUTH_ANONYMOUS_ENABLED: "true"
    GF_AUTH_ANONYMOUS_ORG_ROLE: Admin
  volumes:
    - ./grafana/provisioning:/etc/grafana/provisioning:ro
```

### 3.5 Prometheus scrape config (`docker/prometheus.yml`)

```yaml
scrape_configs:
  - job_name: 'springboot-mq'
    metrics_path: /actuator/prometheus
    scrape_interval: 5s
    static_configs:
      - targets: ['host.docker.internal:8080']
```

핵심: **`host.docker.internal:8080`**.
- Spring Boot 앱은 호스트에서 실행 (컨테이너 아님)
- Prometheus는 컨테이너 안
- 컨테이너에서 호스트로 접근하는 표준 호스트명이 `host.docker.internal`
- macOS Docker Desktop / OrbStack은 기본 지원, Linux는 `extra_hosts: host-gateway` 매핑 필요 (compose에 추가됨)

### 3.6 Grafana 자동 datasource provisioning

```
docker/grafana/provisioning/datasources/prometheus.yml
```

```yaml
apiVersion: 1
datasources:
  - name: Prometheus
    type: prometheus
    url: http://prometheus:9090
    isDefault: true
```

이 파일을 `/etc/grafana/provisioning/datasources/`로 마운트하면 컨테이너 기동 시 자동으로 datasource가 만들어진다 — UI에서 수동으로 추가할 필요 없음.

Grafana 내부에서는 `prometheus` 컨테이너에 같은 docker network로 붙어있으므로 `http://prometheus:9090` 으로 접근.

## 4. 검증 결과

### 4.1 Boot 앱 /actuator/prometheus 응답

```
kafka_app_info_start_time_ms{client_id="consumer-springboot-mq-batch-consumer-5",...}
kafka_consumer_commit_sync_time_ns_total{client_id="...",spring_id="batchConsumerFactory...."}
...
```

→ 우리가 만든 6개 consumer + 1 producer + 1 DLQ producer가 각각 별도 client_id로 메트릭 노출.

### 4.2 Prometheus targets 상태

```
GET http://localhost:9090/api/v1/targets
→ "http://host.docker.internal:8080/actuator/prometheus" health=up
```

### 4.3 실제 쿼리 검증

burst 30개 보낸 직후:
```
kafka_producer_record_send_total{client_id="springboot-mq-producer-1"} = 30
```

→ Producer가 정확히 30번 send한 것이 메트릭으로 카운트됨.

### 4.4 Grafana datasource

```
GET http://localhost:3000/api/datasources
→ Prometheus -> http://prometheus:9090 (type=prometheus)
```

provisioning 자동 등록 확인.

## 5. 학습 포인트

### 5.1 운영에서 가장 가치 있는 지표는 lag

`kafka_consumer_lag_records{client_id=...}` 가 가장 중요:
- 0 또는 안정값으로 머무름 = 처리 따라잡음
- 시간에 따라 증가 = stuck consumer / 처리 속도 부족
- spike 후 감소 = 정상 burst 복구

운영 알람은 보통 **"lag이 N분 이상 증가 추세"** 같은 형태로 설정.

### 5.2 throughput과 error rate

- `rate(kafka_producer_record_send_total[1m])`: 분당 송신 속도
- `rate(kafka_consumer_records_consumed_total[1m])`: 분당 소비 속도
- `rate(kafka_producer_record_error_total[1m])` / `rate(kafka_producer_record_send_total[1m])`: 송신 실패율

이 세 가지로 정상/비정상을 빠르게 판단.

### 5.3 client_id / spring_id 라벨이 비결

여러 ContainerFactory를 만들었을 때 (D-2, D-3에서 그랬듯), 같은 메트릭이라도 어느 consumer인지 구분할 수 있어야 의미 있다. `MicrometerConsumerListener`가 자동으로 부착하는 `spring_id` 라벨 덕에 factory 이름별로 차트를 분리할 수 있다.

### 5.4 메트릭 카디널리티 주의

라벨 조합이 폭발하면 (예: `user_id` 같은 고-카디널리티 라벨) Prometheus 메모리/디스크가 폭증. **메시지 key를 라벨로 쓰지 말 것.** 메트릭은 집계 단위 (group, topic, status) 에만 카디널리티가 있어야 함.

### 5.5 Grafana 익명 Admin 모드

학습 환경이라 `GF_AUTH_ANONYMOUS_ENABLED=true`로 로그인 없이 모든 권한 부여. 운영에서는 절대 금지.

## 6. 화면 확인 절차

```bash
# 1. 인프라 + 앱 기동
cd docker && docker compose up -d
./gradlew bootRun

# 2. burst 트래픽
curl -X POST "http://localhost:8080/messages/burst?count=100"

# 3. Prometheus 직접 쿼리
open http://localhost:9090/graph
# 쿼리 예: rate(kafka_producer_record_send_total[1m])

# 4. Grafana 에서 시각화
open http://localhost:3000
# Explore > Prometheus > 쿼리 입력
```

## 7. 남은 TODO / 다음

- [ ] Grafana 대시보드 JSON을 provisioning에 같이 두면 한 번에 차트까지 자동 생성 가능 (이번엔 Explore에서 직접 쿼리하는 수준에서 멈춤)
- [ ] DLT 발행 횟수 / consumer lag 알람 만들기
- F-2: Converter pattern + sealed event (마지막 단계)
