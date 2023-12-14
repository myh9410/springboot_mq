package com.springboot.mq.domains.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.springboot.mq.common.enums.YorN;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "boards")
@Entity
public class Boards {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    @Column(name = "user_no")
    private Long userNo;

    private String title;

    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "private")
    private YorN isPrivate;

    @CreatedDate
    @Column(name = "create_date")
    private LocalDateTime createDate;

    @LastModifiedDate
    @Column(name = "update_date")
    private LocalDateTime updateDate;
}
