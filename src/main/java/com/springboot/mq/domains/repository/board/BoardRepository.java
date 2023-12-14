package com.springboot.mq.domains.repository.board;

import com.springboot.mq.domains.domain.Boards;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<Boards, Long>, BoardCustomRepository {
}
