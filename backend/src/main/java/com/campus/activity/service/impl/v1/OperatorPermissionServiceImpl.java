package com.campus.activity.service.impl.v1;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.v1.OperatorPermissionService;
import org.springframework.stereotype.Service;

@Service
public class OperatorPermissionServiceImpl implements OperatorPermissionService {
    private final UserMapper userMapper;

    public OperatorPermissionServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

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

    @Override
    public void requireRole(User operator, UserRole requiredRole) {
        if (!requiredRole.equals(operator.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "permission denied for role: " + operator.getRole());
        }
    }

    @Override
    public void requireSelfOrAdmin(User operator, Long targetUserId) {
        if (!operator.getId().equals(targetUserId) && !UserRole.ADMIN.equals(operator.getRole())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only self or admin can access this resource");
        }
    }

    @Override
    public void requireSelf(User operator, Long targetUserId) {
        if (!operator.getId().equals(targetUserId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "only self can access this resource");
        }
    }
}
