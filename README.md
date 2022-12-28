# springboot_mq


### 명령어
- 도커 kafka 이미지 실행
  - docker exec -it kafka bash
- 토픽 조회 
  - kafka-topics.sh --list --bootstrap-server {server_name}:{port}
- 토픽 제거
  - kafka-topics.sh --delete --bootstrap-server {server_name}:{port} --topic {topic_name}
- 컨슈머 확인
  - kafka-console-consumer.sh --bootstrap-server {server_name}:{port} --topic {topic_name} --from-beginning --partition {partition_no}