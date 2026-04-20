package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.User;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for users.
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * Read user by username (unique key).
     */
    User selectByUsername(@Param("username") String username);

    /**
     * Read user by openid (unique key, nullable in DB).
     */
    User selectByOpenid(@Param("openid") String openid);

    /**
     * Read user by phone.
     */
    User selectByPhone(@Param("phone") String phone);
}
