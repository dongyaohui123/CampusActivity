package com.campus.activity.service.v1;

import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;

/**
 * OperatorPermissionService服务接口。
 */
public interface OperatorPermissionService {
    /**
     * 校验操作人身份与角色。
     *
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 操作人实体
     */
    User verifyOperator(Long operatorUserId, UserRole operatorRole);

    /**
     * 要求操作人具备指定角色。
     *
     * @param operator 操作人
     * @param requiredRole 期望角色
     */
    void requireRole(User operator, UserRole requiredRole);

    /**
     * 要求操作人是本人或管理员。
     *
     * @param operator 操作人
     * @param targetUserId 目标用户 ID
     */
    void requireSelfOrAdmin(User operator, Long targetUserId);

    /**
     * 要求操作人是本人。
     *
     * @param operator 操作人
     * @param targetUserId 目标用户 ID
     */
    void requireSelf(User operator, Long targetUserId);
}
