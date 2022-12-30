package com.springboot.mq.domains.repository;

import com.springboot.mq.domains.domain.Test;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepository extends JpaRepository<Test, Long> {
}
