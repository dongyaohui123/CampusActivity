package com.campus.activity.service;

import com.campus.activity.common.PageResponse;
import com.campus.activity.dto.user.CreateUserRequest;
import com.campus.activity.dto.user.UpdateUserRequest;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;

/**
 * UserService服务接口。
 */
public interface UserService {
    /**
     * 创建用户。
     *
     * @param request 创建请求
     * @return 用户信息
     */
    User createUser(CreateUserRequest request);

    /**
     * 更新用户。
     *
     * @param id 用户 ID
     * @param request 更新请求
     * @return 用户信息
     */
    User updateUser(Long id, UpdateUserRequest request);

    /**
     * 禁用用户。
     *
     * @param id 用户 ID
     */
    void disableUser(Long id);

    /**
     * 按 ID 查询用户详情。
     *
     * @param id 用户 ID
     * @return 用户信息
     */
    User getUserById(Long id);

    /**
     * 分页查询用户列表。
     *
     * @param username 用户名筛选
     * @param status 状态筛选
     * @param role 角色筛选
     * @param page 页码
     * @param size 每页大小
     * @return 分页数据
     */
    PageResponse<User> listUsers(String username, UserStatus status, UserRole role, long page, long size);
}
