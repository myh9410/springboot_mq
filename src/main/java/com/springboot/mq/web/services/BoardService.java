package com.springboot.mq.web.services;

import com.springboot.mq.common.enums.YorN;
import com.springboot.mq.domains.domain.Boards;
import com.springboot.mq.domains.repository.board.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

@Slf4j
@RequiredArgsConstructor
@Service
public class BoardService {

    private final BoardRepository boardRepository;

    @Transactional(propagation = Propagation.NESTED)
    public Long createWelcomeBoard(Long userNo) {
        Boards welcomeBoard = boardRepository.save(
            Boards.builder()
                    .userNo(userNo)
                    .title("환영합니다.")
                    .content("안녕하세요." + userNo + "님" + "환영합니다.")
                    .isPrivate(YorN.Y)
                    .build()
        );

        throw new RuntimeException();
    }

}
