# springboot_mq

### kafka 명령어
- 도커 kafka 이미지 실행
  - docker exec -it kafka bash
- 토픽 조회 
  - kafka-topics.sh --list --bootstrap-server {server_name}:{port}
- 토픽 제거
  - kafka-topics.sh --delete --bootstrap-server {server_name}:{port} --topic {topic_name}
- 토픽 상세 스펙 확인
  - kafka-topics.sh --bootstrap-server {server_name}:{port} --describe --topic {topic_name}
- 토픽 partition 수 변경
  - kafka-topics.sh --bootstrap-server {server_name}:{port} --alter --topic {topic_name} --partition {new_partition_count}
- 컨슈머 확인
  - kafka-console-consumer.sh --bootstrap-server {server_name}:{port} --topic {topic_name} --from-beginning --partition {partition_no}
- 
### kafka 실행
- .docker 경로 하위에서 docker-compose up -d로 kafka와 zookeeper 이미지 생성
