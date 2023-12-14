package com.springboot.mq.domains.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class DefaultBoardCreateEvent {
    /**
     * kafka에서 dto를 통해 메세지를 produce, consume할 때,
     * org.apache.kafka.common.errors.RecordDeserializationException
     * com.fasterxml.jackson.databind.exc.MismatchedInputException
     * deserialize 과정에서 에러 발생
     * -> jackson을 통한 object mapping 시 AllArgsConstructor와 기본 생성자(NoArgsConstructor)가 없으면 에러
     */
    private Long userNo;
}
