package com.auto.test.service;

import com.auto.test.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 用户服务接口。
 */
public interface UserService extends IService<User> {

    /**
     * 根据用户名查询用户。
     */
    User getByUsername(String username);

    /**
     * 校验原始密码与加密密码。
     */
    boolean checkPassword(String rawPassword, String encodedPassword);

    /**
     * 对密码进行加密。
     */
    String encodePassword(String rawPassword);
}
