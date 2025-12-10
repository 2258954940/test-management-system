package com.auto.test.mapper;

import com.auto.test.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper，继承 MyBatis-Plus 基础 CRUD。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
}
