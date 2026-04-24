package com.campus.activity.service.impl.v1;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.user.UserProfileUpdateRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.service.v1.V1UserService;
import com.campus.activity.view.v1.RegistrationRecordView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现（v1）。
 * 负责用户资料查询/更新与用户报名记录聚合。
 */
@Service
public class V1UserServiceImpl implements V1UserService {
    private final UserMapper userMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final OperatorPermissionService permissionService;

    /**
     * 构造函数。
     */
    public V1UserServiceImpl(
            UserMapper userMapper,
            ActivityRegistrationMapper registrationMapper,
            ActivityMapper activityMapper,
            OperatorPermissionService permissionService
    ) {
        this.userMapper = userMapper;
        this.registrationMapper = registrationMapper;
        this.activityMapper = activityMapper;
        this.permissionService = permissionService;
    }

    /**
     * 查询用户资料。
     *
     * @param userId 目标用户 ID
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 用户资料
     */
    @Override
    public User getUserProfile(Long userId, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireSelfOrAdmin(operator, userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found: " + userId);
        }
        // 返回前移除敏感字段。
        user.setPasswordHash(null);
        return user;
    }

    /**
     * 更新用户资料。
     *
     * @param userId 目标用户 ID
     * @param request 更新请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 更新后的用户资料
     */
    @Override
    @Transactional
    public User updateUserProfile(Long userId, UserProfileUpdateRequest request, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireSelfOrAdmin(operator, userId);

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "user not found: " + userId);
        }
        if (!hasAnyUpdatableField(request)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "no profile field provided");
        }

        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        userMapper.updateById(user);
        user.setPasswordHash(null);
        return user;
    }

    /**
     * 查询用户报名记录。
     *
     * @param userId 目标用户 ID
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 报名记录列表
     */
    @Override
    public List<RegistrationRecordView> getUserRegistrations(Long userId, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireSelfOrAdmin(operator, userId);

        List<ActivityRegistration> registrations = registrationMapper.selectList(
                new QueryWrapper<ActivityRegistration>()
                        .eq("user_id", userId)
                        .orderByDesc("registered_at", "id")
        );
        if (registrations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> activityIds = registrations.stream()
                .map(ActivityRegistration::getActivityId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<Activity> activities = activityIds.isEmpty() ? Collections.emptyList() : activityMapper.selectBatchIds(activityIds);
        Map<Long, Activity> activityMap = activities.stream().collect(Collectors.toMap(Activity::getId, a -> a));

        List<RegistrationRecordView> result = new ArrayList<>(registrations.size());
        for (ActivityRegistration registration : registrations) {
            Activity activity = activityMap.get(registration.getActivityId());
            RegistrationRecordView view = new RegistrationRecordView();
            view.setRegistrationId(registration.getId());
            view.setActivityId(registration.getActivityId());
            view.setRegistrationStatus(registration.getStatus());
            view.setRegisteredAt(registration.getRegisteredAt());
            view.setCancelledAt(registration.getCancelledAt());
            if (activity != null) {
                view.setActivityTitle(activity.getTitle());
                view.setLocation(activity.getLocation());
                view.setActivityStartTime(activity.getStartTime());
                view.setActivityEndTime(activity.getEndTime());
            }
            result.add(view);
        }
        return result;
    }

    /**
     * 判断是否存在可更新字段，避免空更新。
     */
    private boolean hasAnyUpdatableField(UserProfileUpdateRequest request) {
        return StringUtils.hasText(request.getNickname())
                || StringUtils.hasText(request.getAvatarUrl())
                || StringUtils.hasText(request.getPhone())
                || request.getGender() != null;
    }
}
