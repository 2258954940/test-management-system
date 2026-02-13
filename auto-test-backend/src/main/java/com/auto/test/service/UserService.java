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

       /**
     * 修改用户状态（启用/禁用）
     * @param id 用户ID
     * @param status 新状态：1=启用，0=禁用
     */
    boolean updateUserStatus(Long id, Integer status);
}
