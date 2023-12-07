package com.springboot.mq.domains.domain;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "callback")
@Entity
public class Callback {

    @Id
    private Long no;

    private Long count;

    public void addCount() {
        this.count += 1;
    }

}
