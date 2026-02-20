package com.auto.test.service.impl;

import com.auto.test.entity.User;
import com.auto.test.mapper.UserMapper;
import com.auto.test.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类（毕设演示：密码改为明文验证）
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    // 加密器（毕设演示：暂时不用，保留代码用于说明生产环境）
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 根据用户名查询用户
     */
    @Override
    public User getByUsername(String username) {
        if (username != null && username.trim().length() < 2) {
            throw new IllegalArgumentException("用户名长度不能小于2");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return this.baseMapper.selectOne(wrapper);
    }

    /**
     * 校验密码（毕设演示：改为明文对比）
     */
    @Override
    public boolean checkPassword(String rawPassword, String encodedPassword) {
        // 生产环境逻辑：return passwordEncoder.matches(rawPassword, encodedPassword);
        // 毕设演示：明文对比（已注释加密逻辑，数据库存明文）
        return rawPassword.equals(encodedPassword);
    }

    /**
     * 密码加密（毕设演示：暂时返回明文，保留方法避免报错）
     */
    @Override
    public String encodePassword(String rawPassword) {
        // 生产环境逻辑：return passwordEncoder.encode(rawPassword);
        // 毕设演示：返回明文
        return rawPassword;
    }
     /**
     * 用户状态更新（启用/禁用）
     */
        @Override
    public boolean updateUserStatus(Long id, Integer status) {
        User user = new User();
        user.setId(id);
        user.setStatus(status);
        // 调用MyBatis-Plus的updateById更新状态
        return this.updateById(user);
    }

    @Override
    public boolean save(User entity) {
        if (entity != null && entity.getUsername() != null
                && entity.getUsername().trim().length() < 2) {
            throw new IllegalArgumentException("用户名长度不能小于2");
        }
        return super.save(entity);
    }

    @Override
    public boolean updateById(User entity) {
        if (entity != null && entity.getUsername() != null
                && entity.getUsername().trim().length() < 2) {
            throw new IllegalArgumentException("用户名长度不能小于2");
        }
        return super.updateById(entity);
    }
}