package com.springboot.mq.domains.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.querydsl.core.annotations.QueryProjection;
import com.springboot.mq.domains.domain.User;
import lombok.*;

import java.time.LocalDateTime;

@ToString
@Getter
@Builder
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInfo {

    private Long no;
    private String id;

    private String name;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateDate;

    @QueryProjection
    public UserInfo(Long no, String id, String name, LocalDateTime createDate, LocalDateTime updateDate) {
        this.no = no;
        this.id = id;
        this.name = name;
        this.createDate = createDate;
        this.updateDate = updateDate;
    }

    public UserInfo(User user) {
        this.no = user.getNo();
        this.id = user.getId();
        this.name = user.getName();
        this.createDate = user.getCreateDate();
        this.updateDate = user.getUpdateDate();
    }

}
