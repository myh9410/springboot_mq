package com.springboot.mq.domains.repository.customer.user;

import com.springboot.mq.domains.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>, UserCustomRepository {
}
