package com.springboot.mq.domains.repository.callback;

import com.springboot.mq.domains.domain.Callback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CallbackRepository extends JpaRepository<Callback, Long> {
}
