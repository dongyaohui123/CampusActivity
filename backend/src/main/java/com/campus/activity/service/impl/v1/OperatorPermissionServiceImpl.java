package com.campus.activity.service.impl.v1;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.v1.OperatorPermissionService;
import org.springframework.stereotype.Service;

/**
 * 操作人权限校验服务实现。
 * 负责统一校验：操作者身份有效性、角色匹配、是否本人/管理员。
 */
@Service
public class OperatorPermissionServiceImpl implements OperatorPermissionService {
    private final UserMapper userMapper;

    /**
     * 构造函数。
     *
     * @param userMapper 用户数据访问
     */
    public OperatorPermissionServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
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
}
