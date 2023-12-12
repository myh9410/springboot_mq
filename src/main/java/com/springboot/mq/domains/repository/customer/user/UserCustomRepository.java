package com.springboot.mq.domains.repository.customer.user;

import com.springboot.mq.domains.dto.UserInfo;

public interface UserCustomRepository {
    UserInfo getUserInfoByNo(Long no);
}
