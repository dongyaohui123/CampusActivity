package com.campus.activity.service.impl.v1;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityManagerPermissionGrant;
import com.campus.activity.entity.User;
import com.campus.activity.enums.ActivityManagerPermission;
import com.campus.activity.enums.BasicStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityManagerPermissionGrantMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 操作人权限校验服务实现。
 * 负责统一校验：操作者身份有效性、角色匹配、是否本人/管理员。
 */
@Service
public class OperatorPermissionServiceImpl implements OperatorPermissionService {
    private static final Set<ActivityManagerPermission> DEFAULT_MANAGER_PERMISSIONS =
            EnumSet.of(ActivityManagerPermission.VIEW_REGISTRATIONS, ActivityManagerPermission.CHECK_IN);

    private final UserMapper userMapper;
    private final ActivityMapper activityMapper;
    private final ActivityManagerPermissionGrantMapper managerPermissionGrantMapper;

    /**
     * 构造函数。
     *
     * @param userMapper 用户数据访问
     */
    public OperatorPermissionServiceImpl(UserMapper userMapper,
                                         ActivityMapper activityMapper,
                                         ActivityManagerPermissionGrantMapper managerPermissionGrantMapper) {
        this.userMapper = userMapper;
        this.activityMapper = activityMapper;
        this.managerPermissionGrantMapper = managerPermissionGrantMapper;
    }

    /**
     * 校验操作人身份与角色。
     *
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 操作人实体
     */
    @Override
    public User verifyOperator(Long operatorUserId, UserRole operatorRole) {
        if (operatorUserId == null || operatorRole == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "operatorUserId and operatorRole are required");
        }
        User operator = userMapper.selectById(operatorUserId);
        if (operator == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "operator not found");
        }
        if (!UserStatus.ACTIVE.equals(operator.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "operator is not active");
        }
        if (!operatorRole.equals(operator.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "operator role mismatch");
        }
        return operator;
    }

    /**
     * 要求操作人具备指定角色。
     *
     * @param operator 操作人
     * @param requiredRole 要求角色
     */
    @Override
    public void requireRole(User operator, UserRole requiredRole) {
        if (!requiredRole.equals(operator.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "permission denied for role: " + operator.getRole());
        }
    }

    /**
     * 要求操作人是本人或管理员。
     *
     * @param operator 操作人
     * @param targetUserId 目标用户 ID
     */
    @Override
    public void requireSelfOrAdmin(User operator, Long targetUserId) {
        if (!operator.getId().equals(targetUserId) && !UserRole.ADMIN.equals(operator.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only self or admin can access this resource");
        }
    }

    /**
     * 要求操作人必须是本人。
     *
     * @param operator 操作人
     * @param targetUserId 目标用户 ID
     */
    @Override
    public void requireSelf(User operator, Long targetUserId) {
        if (!operator.getId().equals(targetUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only self can access this resource");
        }
    }

    @Override
    public User verifyOrganizerOrActivityManager(Long activityId,
                                                 Long operatorUserId,
                                                 UserRole operatorRole,
                                                 ActivityManagerPermission requiredPermission) {
        User operator = verifyOperator(operatorUserId, operatorRole);
        if (activityId == null || activityId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "activityId must be >= 1");
        }
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + activityId);
        }
        if (UserRole.ORGANIZER.equals(operator.getRole())) {
            if (!operator.getId().equals(activity.getOrganizerId())) {
                throw new BusinessException(ErrorCode.FORBIDDEN, "organizer cannot operate another organizer's activity");
            }
            return operator;
        }

        ActivityManagerPermissionGrant grant = managerPermissionGrantMapper.selectOne(
                new QueryWrapper<ActivityManagerPermissionGrant>()
                        .eq("activity_id", activityId)
                        .eq("user_id", operator.getId())
                        .eq("status", BasicStatus.ACTIVE.name())
                        .last("LIMIT 1")
        );
        if (grant == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "permission denied for this activity");
        }
        Set<ActivityManagerPermission> permissions = decodePermissions(grant.getPermissions());
        if (!permissions.contains(requiredPermission)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "missing activity permission: " + requiredPermission);
        }
        return operator;
    }

    private Set<ActivityManagerPermission> decodePermissions(String raw) {
        if (!StringUtils.hasText(raw)) {
            return EnumSet.copyOf(DEFAULT_MANAGER_PERMISSIONS);
        }
        Set<ActivityManagerPermission> parsed = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .map(value -> {
                    try {
                        return ActivityManagerPermission.valueOf(value);
                    } catch (IllegalArgumentException ex) {
                        return null;
                    }
                })
                .filter(permission -> permission != null)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ActivityManagerPermission.class)));
        if (parsed.isEmpty()) {
            return EnumSet.copyOf(DEFAULT_MANAGER_PERMISSIONS);
        }
        return parsed;
    }
}
