package com.campus.activity.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.common.PageResponse;
import com.campus.activity.dto.user.CreateUserRequest;
import com.campus.activity.dto.user.UpdateUserRequest;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.UserService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * UserServiceImpl服务实现。
 */
@Service
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;

    /**
     * 构造函数。
     *
     * @param userMapper 用户数据访问
     */
    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 创建用户。
     *
     * @param request 创建请求
     * @return 创建后的用户
     */
    @Override
    @Transactional
    public User createUser(CreateUserRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(request.getPasswordHash());
        user.setOpenid(request.getOpenid());
        user.setNickname(request.getNickname());
        user.setAvatarUrl(request.getAvatarUrl());
        user.setGender(request.getGender());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole() != null ? request.getRole() : UserRole.STUDENT);
        user.setStatus(request.getStatus() != null ? request.getStatus() : UserStatus.ACTIVE);
        user.setForcePasswordChange(request.getForcePasswordChange() != null ? request.getForcePasswordChange() : Boolean.FALSE);

        try {
            userMapper.insert(user);
        } catch (DataIntegrityViolationException ex) {
            throw mapConstraintToBusinessException(ex, "create user failed");
        }
        return sanitizeUser(user);
    }

    /**
     * 更新用户。
     *
     * @param id 用户 ID
     * @param request 更新请求
     * @return 更新后的用户
     */
    @Override
    @Transactional
    public User updateUser(Long id, UpdateUserRequest request) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found: " + id);
        }

        if (!hasAnyUpdatableField(request)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "no updatable field provided");
        }

        if (request.getUsername() != null) {
            existing.setUsername(request.getUsername());
        }
        if (request.getPasswordHash() != null) {
            existing.setPasswordHash(request.getPasswordHash());
        }
        if (request.getOpenid() != null) {
            existing.setOpenid(request.getOpenid());
        }
        if (request.getNickname() != null) {
            existing.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            existing.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getGender() != null) {
            existing.setGender(request.getGender());
        }
        if (request.getPhone() != null) {
            existing.setPhone(request.getPhone());
        }
        if (request.getRole() != null) {
            existing.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            existing.setStatus(request.getStatus());
        }
        if (request.getForcePasswordChange() != null) {
            existing.setForcePasswordChange(request.getForcePasswordChange());
        }

        try {
            userMapper.updateById(existing);
        } catch (DataIntegrityViolationException ex) {
            throw mapConstraintToBusinessException(ex, "update user failed");
        }
        return sanitizeUser(existing);
    }

    /**
     * 禁用用户。
     *
     * @param id 用户 ID
     */
    @Override
    @Transactional
    public void disableUser(Long id) {
        User existing = userMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found: " + id);
        }
        existing.setStatus(UserStatus.DISABLED);
        userMapper.updateById(existing);
    }

    /**
     * 查询用户详情。
     *
     * @param id 用户 ID
     * @return 用户详情
     */
    @Override
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found: " + id);
        }
        return sanitizeUser(user);
    }

    /**
     * 按条件分页查询用户。
     *
     * @param username 用户名筛选
     * @param status 状态筛选
     * @param role 角色筛选
     * @param page 页码
     * @param size 每页大小
     * @return 用户分页数据
     */
    @Override
    public PageResponse<User> listUsers(String username, UserStatus status, UserRole role, long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.max(1, Math.min(size, 100));

        LambdaQueryWrapper<User> queryWrapper = Wrappers.lambdaQuery(User.class)
                .like(StringUtils.hasText(username), User::getUsername, username)
                .eq(status != null, User::getStatus, status)
                .eq(role != null, User::getRole, role)
                .orderByDesc(User::getId);

        Page<User> mpPage = new Page<>(safePage, safeSize);
        Page<User> result = userMapper.selectPage(mpPage, queryWrapper);
        List<User> records = result.getRecords().stream().map(this::sanitizeUser).collect(Collectors.toList());
        return new PageResponse<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), records);
    }

    /**
     * 判断请求中是否包含可更新字段。
     */
    private boolean hasAnyUpdatableField(UpdateUserRequest request) {
        return request.getUsername() != null
                || request.getPasswordHash() != null
                || request.getOpenid() != null
                || request.getNickname() != null
                || request.getAvatarUrl() != null
                || request.getGender() != null
                || request.getPhone() != null
                || request.getRole() != null
                || request.getStatus() != null
                || request.getForcePasswordChange() != null;
    }

    /**
     * 脱敏用户敏感字段。
     */
    private User sanitizeUser(User user) {
        User sanitized = new User();
        BeanUtils.copyProperties(user, sanitized);
        sanitized.setPasswordHash(null);
        return sanitized;
    }

    /**
     * 将唯一约束异常映射为业务异常。
     */
    private BusinessException mapConstraintToBusinessException(Exception ex, String fallbackMessage) {
        String message = ex.getMessage();
        if (message != null) {
            if (message.contains("uk_users_username")) {
                return new BusinessException(ErrorCode.CONFLICT, "username already exists");
            }
            if (message.contains("uk_users_openid")) {
                return new BusinessException(ErrorCode.CONFLICT, "openid already exists");
            }
        }
        return new BusinessException(ErrorCode.CONFLICT, fallbackMessage);
    }
}
