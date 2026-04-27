package com.campus.activity.service.impl.v1;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.registration.RegistrationCancelRequest;
import com.campus.activity.dto.v1.registration.RegistrationCreateRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.User;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.RegistrationStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.service.v1.V1RegistrationService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报名服务实现（v1）。
 * 负责学生报名与取消报名，并校验活动可报名条件。
 */
@Service
public class V1RegistrationServiceImpl implements V1RegistrationService {
    private final ActivityMapper activityMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final ActivityReviewMapper reviewMapper;
    private final OperatorPermissionService permissionService;

    /**
     * 构造函数。
     */
    public V1RegistrationServiceImpl(
            ActivityMapper activityMapper,
            ActivityRegistrationMapper registrationMapper,
            ActivityReviewMapper reviewMapper,
            OperatorPermissionService permissionService
    ) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.reviewMapper = reviewMapper;
        this.permissionService = permissionService;
    }

    /**
     * 学生报名活动。
     *
     * @param request 报名请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 报名记录
     */
    @Override
    @Transactional
    public ActivityRegistration register(RegistrationCreateRequest request, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.STUDENT);

        Activity activity = activityMapper.selectById(request.getActivityId());
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + request.getActivityId());
        }
        if (ActivityStatus.CANCELLED.equals(activity.getStatus()) || ActivityStatus.DRAFT.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity cannot be registered now");
        }
        if (activity.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(activity.getRegistrationDeadline())) {
            throw new BusinessException(ErrorCode.CONFLICT, "registration deadline has passed");
        }

        // 报名前必须是审核通过活动。
        ActivityReview review = reviewMapper.selectById(activity.getId());
        if (review == null || !ReviewStatus.APPROVED.equals(review.getReviewStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity has not been approved");
        }

        ActivityRegistration existing = registrationMapper.selectOne(
                new QueryWrapper<ActivityRegistration>()
                        .eq("activity_id", activity.getId())
                        .eq("user_id", operator.getId())
                        .last("LIMIT 1")
        );
        if (existing == null) {
            ActivityRegistration registration = new ActivityRegistration();
            registration.setActivityId(activity.getId());
            registration.setUserId(operator.getId());
            registration.setStatus(RegistrationStatus.REGISTERED);
            registration.setRemark(request.getRemark());
            registration.setRegisteredAt(LocalDateTime.now());
            registrationMapper.insert(registration);
            return registration;
        }

        // 已报名/已签到状态不允许重复报名，已取消记录允许恢复为报名状态。
        if (RegistrationStatus.REGISTERED.equals(existing.getStatus())
                || RegistrationStatus.CHECKED_IN.equals(existing.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "already registered");
        }

        existing.setStatus(RegistrationStatus.REGISTERED);
        existing.setRemark(request.getRemark());
        existing.setCancelledAt(null);
        existing.setRegisteredAt(LocalDateTime.now());
        registrationMapper.updateById(existing);
        return existing;
    }

    /**
     * 学生取消报名。
     *
     * @param registrationId 报名记录 ID
     * @param request 取消请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 更新后的报名记录
     */
    @Override
    @Transactional
    public ActivityRegistration cancelRegistration(
            Long registrationId,
            RegistrationCancelRequest request,
            Long operatorUserId,
            UserRole operatorRole
    ) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.STUDENT);

        ActivityRegistration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "registration not found: " + registrationId);
        }
        permissionService.requireSelf(operator, registration.getUserId());
        if (RegistrationStatus.CANCELLED.equals(registration.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "registration already cancelled");
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        if (request != null && request.getRemark() != null) {
            registration.setRemark(request.getRemark());
        }
        registrationMapper.updateById(registration);
        return registration;
    }
}
