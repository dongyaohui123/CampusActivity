package com.campus.activity.service.v1;

import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;

public interface OperatorPermissionService {
    User verifyOperator(Long operatorUserId, UserRole operatorRole);

    void requireRole(User operator, UserRole requiredRole);

    void requireSelfOrAdmin(User operator, Long targetUserId);

    void requireSelf(User operator, Long targetUserId);
}
