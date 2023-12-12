package com.springboot.mq.domains.repository.customer.user;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.springboot.mq.domains.dto.QUserInfo;
import com.springboot.mq.domains.dto.UserInfo;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.persistence.EntityManager;

import static com.springboot.mq.domains.domain.QUser.user;

public class UserCustomRepositoryImpl implements UserCustomRepository {

    private final JPAQueryFactory queryFactory;

    public UserCustomRepositoryImpl(@Qualifier(value = "customerEntityManager") EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    public UserInfo getUserInfoByNo(Long no) {
        return queryFactory.select(
                    new QUserInfo(
                            user.no, user.id, user.name, user.createDate, user.updateDate
                    ))
                .from(user)
                .where(user.no.eq(no)).fetchFirst();
    }
}
