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
import com.campus.activity.service.RegistrationTicketService;
import com.campus.activity.service.v1.ActivityPhaseResolver;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.service.v1.V1RegistrationService;
import com.campus.activity.view.v1.TicketDetailView;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final RegistrationTicketService registrationTicketService;
    private final ActivityPhaseResolver activityPhaseResolver;

    /**
     * 构造函数。
     */
    public V1RegistrationServiceImpl(
            ActivityMapper activityMapper,
            ActivityRegistrationMapper registrationMapper,
            ActivityReviewMapper reviewMapper,
            OperatorPermissionService permissionService,
            RegistrationTicketService registrationTicketService,
            ActivityPhaseResolver activityPhaseResolver
    ) {
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.reviewMapper = reviewMapper;
        this.permissionService = permissionService;
        this.registrationTicketService = registrationTicketService;
        this.activityPhaseResolver = activityPhaseResolver;
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
        ActivityStatus effectiveStatus = activityPhaseResolver.resolve(activity);
        if (!ActivityStatus.REGISTRATION_OPEN.equals(effectiveStatus)) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity cannot be registered now");
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
            issueTicket(registration);
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
        existing.setCheckinAt(null);
        existing.setCheckinOperatorId(null);
        issueTicket(existing);
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
        if (RegistrationStatus.CHECKED_IN.equals(registration.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "checked-in registration cannot be cancelled");
        }
        if (RegistrationStatus.CANCELLED.equals(registration.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "registration already cancelled");
        }

        Activity activity = activityMapper.selectById(registration.getActivityId());
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + registration.getActivityId());
        }
        ActivityStatus effectiveStatus = activityPhaseResolver.resolve(activity);
        if (ActivityStatus.ONGOING.equals(effectiveStatus)
                || ActivityStatus.FINISHED.equals(effectiveStatus)
                || ActivityStatus.CANCELLED.equals(effectiveStatus)) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity has started and registration cannot be cancelled");
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(LocalDateTime.now());
        registration.setTicketCode(null);
        registration.setTicketIssuedAt(null);
        registration.setCheckinAt(null);
        registration.setCheckinOperatorId(null);
        if (request != null && request.getRemark() != null) {
            registration.setRemark(request.getRemark());
        }
        registrationMapper.updateById(registration);
        return registration;
    }

    /**
     * 查询电子票详情。
     */
    @Override
    public TicketDetailView getTicketDetail(Long registrationId, Long operatorUserId, UserRole operatorRole) {
        ActivityRegistration registration = getAccessibleRegistration(registrationId, operatorUserId, operatorRole);
        registration = ensureTicketIssuedForActiveRegistration(registration);

        Activity activity = activityMapper.selectById(registration.getActivityId());
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + registration.getActivityId());
        }

        TicketDetailView view = new TicketDetailView();
        view.setRegistrationId(registration.getId());
        view.setActivityId(registration.getActivityId());
        view.setActivityTitle(activity.getTitle());
        view.setLocation(activity.getLocation());
        view.setActivityStartTime(activity.getStartTime());
        view.setActivityEndTime(activity.getEndTime());
        view.setRegistrationStatus(registration.getStatus());
        view.setTicketCode(registration.getTicketCode());
        view.setTicketIssuedAt(registration.getTicketIssuedAt());
        view.setCheckinAt(registration.getCheckinAt());
        view.setQrContent(registrationTicketService.buildQrContent(registration.getActivityId(), registration.getTicketCode()));
        return view;
    }

    /**
     * 输出电子票二维码图片。
     */
    @Override
    public byte[] renderTicketQrCode(Long registrationId, Long operatorUserId, UserRole operatorRole) {
        TicketDetailView detail = getTicketDetail(registrationId, operatorUserId, operatorRole);
        return registrationTicketService.renderQrCode(detail.getQrContent());
    }

    private void issueTicket(ActivityRegistration registration) {
        registration.setTicketCode(generateUniqueTicketCode());
        registration.setTicketIssuedAt(LocalDateTime.now());
    }

    private String generateUniqueTicketCode() {
        for (int i = 0; i < 5; i++) {
            String code = registrationTicketService.generateTicketCode();
            Long existingCount = registrationMapper.selectCount(
                    new QueryWrapper<ActivityRegistration>().eq("ticket_code", code)
            );
            if (existingCount == null || existingCount == 0L) {
                return code;
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "failed to issue unique ticket code");
    }

    private ActivityRegistration ensureTicketIssuedForActiveRegistration(ActivityRegistration registration) {
        if (StringUtils.hasText(registration.getTicketCode())) {
            return registration;
        }
        if (!RegistrationStatus.REGISTERED.equals(registration.getStatus())
                && !RegistrationStatus.CHECKED_IN.equals(registration.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "ticket has not been issued");
        }

        issueTicket(registration);
        registrationMapper.updateById(registration);
        return registration;
    }

    private ActivityRegistration getAccessibleRegistration(Long registrationId, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        ActivityRegistration registration = registrationMapper.selectById(registrationId);
        if (registration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "registration not found: " + registrationId);
        }
        permissionService.requireSelfOrAdmin(operator, registration.getUserId());
        return registration;
    }
}
