# 2026-06-03 — F-2: Sealed Event + Converter 패턴

> 1차 draft. 추후 다시 정리할 예정.

## 1. 목표

토픽과 이벤트 타입이 늘어났을 때 if/else로 분기하지 않도록 **타입 안전한 dispatch 구조** 를 도입한다. 핵심 3축:

1. `sealed interface MqEvent` — 모든 도메인 이벤트의 공통 마커, 닫혀있어서 새 타입 추가 시 컴파일 시점에 강제
2. `EventType` enum — 토픽 ↔ 클래스 매핑의 단일 진실 공급원
3. `MqEventConverter<T>` 인터페이스 + 토픽별 구현 — Spring DI로 모아서 dispatch

## 2. 추가 / 변경된 구조

```
io.github.myh9410.mq/
├── message/
│   ├── MqEvent.java               # sealed interface permits Message, OrderEvent
│   ├── Message.java               # implements MqEvent (기존 record 수정)
│   ├── OrderEvent.java            # 새 이벤트 (id, item, quantity, occurredAt)
│   ├── EventType.java             # enum MESSAGE/ORDER — topic ↔ class 양방향 lookup
│   ├── Topics.java                # ORDERS 상수 추가
│   └── TopicConfig.java           # ordersTopic NewTopic 추가
├── producer/
│   ├── EventPublisher.java        # MessageProducer를 대체. MqEvent → EventType → topic 자동 라우팅
│   ├── MessageController.java     # EventPublisher 사용
│   └── OrderController.java       # 새 엔드포인트 POST /orders
└── consumer/
    ├── MqEventConverter.java      # 인터페이스
    ├── converter/
    │   ├── MessageEventConverter.java
    │   └── OrderEventConverter.java
    └── EventDispatchingConsumer.java   # 디스패처
```

## 3. 핵심 코드

### 3.1 sealed MqEvent + permits

```java
public sealed interface MqEvent permits Message, OrderEvent {
    String id();
}

public record Message(String id, String content, Instant occurredAt) implements MqEvent {}
public record OrderEvent(String id, String item, int quantity, Instant occurredAt) implements MqEvent {}
```

→ 새 이벤트를 추가하려면 (a) record 생성 (b) `permits` 절에 추가 (c) `EventType` enum 추가 (d) `MqEventConverter` 구현 4단계. 이 중 하나라도 빠지면 컴파일 에러나 런타임 에러로 잡힘.

### 3.2 EventType — 양방향 매핑

```java
public enum EventType {
    MESSAGE(Topics.MESSAGES, Message.class),
    ORDER(Topics.ORDERS, OrderEvent.class);

    public static EventType fromTopic(String topic) { ... }
    public static EventType fromClass(Class<? extends MqEvent> clazz) { ... }
}
```

`fromClass`는 producer가 쓰고 (어디로 발행할지), `fromTopic`은 consumer가 쓴다 (어떤 타입으로 파싱할지).

### 3.3 EventPublisher — 클래스만 알면 토픽 자동 결정

```java
public void publish(MqEvent event) {
    EventType type = EventType.fromClass(event.getClass());
    log.info("Publishing: topic={} key={} payload={}", type.topic(), event.id(), event);
    kafkaTemplate.send(type.topic(), event.id(), event);
}
```

호출부는 `publisher.publish(orderEvent)` 한 줄. 토픽 이름을 코드에서 다룰 일이 없어진다.

### 3.4 EventDispatchingConsumer — 토픽 여러 개를 한 listener로

```java
public EventDispatchingConsumer(JsonMapper jsonMapper, List<MqEventConverter<?>> converters) {
    this.converterByClass = converters.stream()
        .collect(Collectors.toUnmodifiableMap(
            MqEventConverter::eventType,
            c -> c,
            (a, b) -> { throw new IllegalStateException("duplicate converter..."); }
        ));
}

@KafkaListener(
    topics = { Topics.MESSAGES, Topics.ORDERS },
    groupId = "springboot-mq-dispatch-consumer",
    containerFactory = "stringKafkaListenerContainerFactory"   // raw String value
)
public void onEvent(ConsumerRecord<String, String> record) {
    EventType type = EventType.fromTopic(record.topic());
    Class<? extends MqEvent> clazz = type.eventClass();
    MqEvent event = jsonMapper.readValue(record.value(), clazz);
    dispatch(event);
}
```

핵심 트릭: **`containerFactory = "stringKafkaListenerContainerFactory"`** — 토픽마다 다른 타입을 받아야 하니까 raw String으로 받아서 토픽별로 JSON 파싱한다. JacksonJsonDeserializer는 한 타입에 고정되어서 여러 토픽 다른 타입 디스패치에는 직접 못 쓴다 (type info 헤더를 쓰지 않는 한).

### 3.5 Converter — `List<MqEventConverter<?>>` 가 모이는 마법

Spring은 `MqEventConverter` 인터페이스를 구현한 `@Component`를 전부 모아 List로 주입해준다. 등록은 `@Component` 어노테이션 하나면 끝. 새 컨버터를 추가하면 디스패처는 코드 한 줄도 안 바꿔도 자동 인식.

```java
@Component
public class OrderEventConverter implements MqEventConverter<OrderEvent> {
    @Override public Class<OrderEvent> eventType() { return OrderEvent.class; }
    @Override public void handle(OrderEvent event) { ... }
}
```

생성자에서 `Map<Class, Converter>` 로 인덱싱하면서 중복 검출까지 함께 — 같은 타입에 컨버터가 2개면 startup 시점에 throw.

## 4. 검증 결과

```
EventDispatchingConsumer wired with 2 converters: [Message, OrderEvent]

POST /messages {"id":"f2-msg-001","content":"hello f2",...}
→ Publishing: topic=messages key=f2-msg-001 payload=Message[...]
→ MessageEventConverter handled: id=f2-msg-001 content=hello f2

POST /orders {"id":"f2-ord-001","item":"book","quantity":2,...}
→ Publishing: topic=orders key=f2-ord-001 payload=OrderEvent[...]
→ OrderEventConverter handled: id=f2-ord-001 item=book quantity=2
```

같은 디스패처가 두 토픽을 받아 각각 다른 컨버터로 위임. 동시에 `MessageConsumer` (group: springboot-mq-consumer) 같은 기존 listener도 별도 group이라 영향 없이 자기 처리 계속.

## 5. 학습 포인트

### 5.1 sealed가 가지는 약속

sealed로 닫아두면 새 이벤트 추가 시 IDE / 컴파일러 / switch exhaustiveness 가 모두 빠진 곳을 알려준다. open class hierarchy의 함정(분기 어딘가에서 새 케이스 처리 누락)을 막아준다.

Switch on sealed:
```java
return switch (event) {
    case Message m -> handleMessage(m);
    case OrderEvent o -> handleOrder(o);
    // 새 sealed 케이스 추가 시 여기서 컴파일 에러
};
```

이번 코드에서는 enum + Map 방식을 썼지만, switch 패턴 매칭도 동등하게 가능. 케이스가 적고 정적이면 switch가 더 명료.

### 5.2 토픽 여러 개를 한 listener가 받을 수 있다

`@KafkaListener(topics = { ... })` 배열로 여러 토픽을 한 메서드에 묶을 수 있다. 디스패처 패턴의 전제 조건이며, 같은 group 안에서 partition 분배가 토픽 단위로 일어난다.

### 5.3 raw String → typed 변환을 listener 안에서 직접

`stringKafkaListenerContainerFactory` (값을 String으로 받는 factory) 를 사용한 게 핵심. 이 layout의 트레이드오프:

| | 장점 | 약점 |
|---|---|---|
| ErrorHandlingDeserializer + JacksonJsonDeserializer (기존) | 자동 deserialization, type-safe payload | 한 factory = 한 target type |
| String + manual readValue (이번) | 토픽별 다른 타입 가능, 디스패치 유연 | listener 안에서 예외 처리 명시 필요 |

### 5.4 Spring DI 컬렉션 주입

```java
public EventDispatchingConsumer(List<MqEventConverter<?>> converters) { ... }
```

이 한 줄로 Spring이 컨버터 빈을 자동 수집. **plugin 점접점**의 대표적 형태 — 도메인 이벤트가 100개로 늘어도 디스패처는 손대지 않는다.

### 5.5 grip-marketing이 동일 패턴을 쓰는 이유

이 구조의 진가는 **이벤트가 5개 이상** 일 때 드러난다. 토픽 → 클래스 → 컨버터를 한 곳에서 등록/관리하면, 새 이벤트 추가 시 분기/라우팅 코드를 안 만들어도 되니까 PR diff가 도메인 코드에만 집중된다.

## 6. F-1을 스킵한 영향

F-1 (Gradle 멀티모듈 분리) 을 스킵했기 때문에 producer / consumer / converter / event가 모두 같은 모듈 안에 있다. 멀티 모듈이라면 `lib-event` 모듈에 MqEvent / EventType / Topics를 두고 producer와 consumer 모듈이 각각 의존하는 형태가 됐을 것. 학습 환경에서는 단일 모듈이 충분.

## 7. 시나리오 전체 정리 (현재까지)

| 단계 | 주제 | 결과 |
|---|---|---|
| A-1 | Producer/Consumer 기본 | String 메시지 round-trip |
| A-2 | JSON DTO | record + JacksonJsonSerializer |
| C-1+C-2 | 에러 핸들러 + DLQ | retry → DLT 자동 발행 |
| D-1 | Partition 분배 | concurrency=3, key 기반 라우팅 |
| B-1 | Idempotent Producer | acks=all + idempotence |
| B-2 | ProducerListener | send 결과 로깅 일원화 |
| D-2 | Batch Listener | List<ConsumerRecord> 패턴 |
| D-3 | Manual ack | offset commit 시점 제어 |
| E | Micrometer + Prometheus + Grafana | 메트릭 파이프라인 |
| F-2 | Converter 패턴 | sealed + EventType + DI 컬렉션 (이번) |

각 단계의 코드/노트는 모두 별도 커밋으로 분리되어 있어 git log를 따라가면 학습 순서가 그대로 따라온다.
