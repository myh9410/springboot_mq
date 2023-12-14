package com.springboot.mq.domains.domain;

import com.atomikos.util.Assert;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@ToString
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "user")
@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long no;

    private String id;

    private String name;

    @Column(name = "password", columnDefinition = "varchar2")
    private String password;

    @CreatedDate
    @Column(name = "create_date", columnDefinition = "datetime")
    private LocalDateTime createDate;

    @LastModifiedDate
    @Column(name = "update_date", columnDefinition = "datetime")
    private LocalDateTime updateDate;

    public static User createUser(String id, String name, String password) {
        return User.builder()
                .id(id)
                .name(name)
                .password(password)
                .build();
    }

    public void changeUpdateDateToNow() {
        this.updateDate = LocalDateTime.now();
    }
}
