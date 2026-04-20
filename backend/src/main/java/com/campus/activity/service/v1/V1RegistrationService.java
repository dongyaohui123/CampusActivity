package com.campus.activity.service.v1;

import com.campus.activity.dto.v1.registration.RegistrationCancelRequest;
import com.campus.activity.dto.v1.registration.RegistrationCreateRequest;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.enums.UserRole;

/**
 * V1RegistrationService服务接口。
 */
public interface V1RegistrationService {
    /**
     * 创建报名记录。
     *
     * @param request 报名请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 报名记录
     */
    ActivityRegistration register(RegistrationCreateRequest request, Long operatorUserId, UserRole operatorRole);

    /**
     * 取消报名记录。
     *
     * @param registrationId 报名记录 ID
     * @param request 取消请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 更新后的报名记录
     */
    ActivityRegistration cancelRegistration(Long registrationId, RegistrationCancelRequest request, Long operatorUserId, UserRole operatorRole);
}
