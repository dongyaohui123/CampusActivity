package com.campus.activity.service.impl.v1;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.activity.OrganizerActivityCreateRequest;
import com.campus.activity.dto.v1.activity.OrganizerActivityCheckinRequest;
import com.campus.activity.dto.v1.activity.OrganizerActivityUpdateRequest;
import com.campus.activity.dto.v1.activity.SubmitReviewRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityAuditLog;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.User;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.AuditAction;
import com.campus.activity.enums.RegistrationStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.Visibility;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityAuditLogMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.service.v1.V1OrganizerActivityService;
import com.campus.activity.view.v1.ActivityRegistrationUserView;
import com.campus.activity.view.v1.CheckinResultView;
import java.time.LocalDateTime;
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
 * 组织者活动服务实现（v1）。
 * 提供组织者创建、修改、提审、列表查询以及报名用户查询能力。
 */
@Service
public class V1OrganizerActivityServiceImpl implements V1OrganizerActivityService {
    private final ActivityMapper activityMapper;
    private final ActivityReviewMapper reviewMapper;
    private final ActivityAuditLogMapper auditLogMapper;
    private final ActivityRegistrationMapper registrationMapper;
    private final UserMapper userMapper;
    private final OperatorPermissionService permissionService;

    /**
     * 构造函数。
     */
    public V1OrganizerActivityServiceImpl(
            ActivityMapper activityMapper,
            ActivityReviewMapper reviewMapper,
            ActivityAuditLogMapper auditLogMapper,
            ActivityRegistrationMapper registrationMapper,
            UserMapper userMapper,
            OperatorPermissionService permissionService
    ) {
        this.activityMapper = activityMapper;
        this.reviewMapper = reviewMapper;
        this.auditLogMapper = auditLogMapper;
        this.registrationMapper = registrationMapper;
        this.userMapper = userMapper;
        this.permissionService = permissionService;
    }

    /**
     * 组织者创建活动。
     *
     * @param request 创建请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 活动信息
     */
    @Override
    @Transactional
    public Activity createActivity(OrganizerActivityCreateRequest request, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);
        validateActivityTime(request.getStartTime(), request.getEndTime(), request.getRegistrationDeadline());

        Activity activity = new Activity();
        activity.setOrganizerId(operator.getId());
        activity.setPublisherId(operator.getId());
        activity.setTitle(request.getTitle());
        activity.setSummary(request.getSummary());
        activity.setContent(request.getContent());
        activity.setCoverUrl(request.getCoverUrl());
        activity.setLocation(request.getLocation());
        activity.setStartTime(request.getStartTime());
        activity.setEndTime(request.getEndTime());
        activity.setRegistrationDeadline(request.getRegistrationDeadline());
        activity.setMaxParticipants(request.getMaxParticipants());
        activity.setRegisteredCount(0);
        activity.setVisibility(request.getVisibility() != null ? request.getVisibility() : Visibility.PUBLIC);
        activity.setFeatured(request.getFeatured() != null ? request.getFeatured() : Boolean.FALSE);
        activity.setStatus(ActivityStatus.DRAFT);
        activityMapper.insert(activity);
        return activity;
    }

    /**
     * 组织者更新活动。
     *
     * @param activityId 活动 ID
     * @param request 更新请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 活动信息
     */
    @Override
    @Transactional
    public Activity updateActivity(Long activityId, OrganizerActivityUpdateRequest request, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);

        Activity activity = getOwnedActivityOrThrow(activityId, operator.getId());
        if (ActivityStatus.CANCELLED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "cancelled activity cannot be updated");
        }
        if (!hasAnyUpdatableField(request)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "no updatable field provided");
        }

        // 更新时间前先合并新旧时间，统一做时间窗口校验。
        LocalDateTime mergedStart = request.getStartTime() != null ? request.getStartTime() : activity.getStartTime();
        LocalDateTime mergedEnd = request.getEndTime() != null ? request.getEndTime() : activity.getEndTime();
        LocalDateTime mergedDeadline = request.getRegistrationDeadline() != null
                ? request.getRegistrationDeadline() : activity.getRegistrationDeadline();
        validateActivityTime(mergedStart, mergedEnd, mergedDeadline);

        if (request.getTitle() != null) {
            activity.setTitle(request.getTitle());
        }
        if (request.getSummary() != null) {
            activity.setSummary(request.getSummary());
        }
        if (request.getContent() != null) {
            activity.setContent(request.getContent());
        }
        if (request.getCoverUrl() != null) {
            activity.setCoverUrl(request.getCoverUrl());
        }
        if (request.getLocation() != null) {
            activity.setLocation(request.getLocation());
        }
        if (request.getStartTime() != null) {
            activity.setStartTime(request.getStartTime());
        }
        if (request.getEndTime() != null) {
            activity.setEndTime(request.getEndTime());
        }
        if (request.getRegistrationDeadline() != null) {
            activity.setRegistrationDeadline(request.getRegistrationDeadline());
        }
        if (request.getMaxParticipants() != null) {
            activity.setMaxParticipants(request.getMaxParticipants());
        }
        if (request.getVisibility() != null) {
            activity.setVisibility(request.getVisibility());
        }
        if (request.getFeatured() != null) {
            activity.setFeatured(request.getFeatured());
        }

        activityMapper.updateById(activity);
        return activity;
    }

    /**
     * 组织者提交审核。
     *
     * @param activityId 活动 ID
     * @param request 提交审核请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 活动信息
     */
    @Override
    @Transactional
    public Activity submitForReview(Long activityId, SubmitReviewRequest request, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);
        Activity activity = getOwnedActivityOrThrow(activityId, operator.getId());
        if (ActivityStatus.CANCELLED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "cancelled activity cannot be submitted");
        }

        // 提审后活动进入可发布状态，同时写入/重置审核记录。
        activity.setStatus(ActivityStatus.PUBLISHED);
        if (activity.getPublishedAt() == null) {
            activity.setPublishedAt(LocalDateTime.now());
        }
        activityMapper.updateById(activity);

        ActivityReview review = reviewMapper.selectById(activityId);
        if (review == null) {
            review = new ActivityReview();
            review.setActivityId(activityId);
            review.setReviewStatus(ReviewStatus.PENDING);
            reviewMapper.insert(review);
        } else {
            review.setReviewStatus(ReviewStatus.PENDING);
            review.setReviewerId(null);
            review.setReviewComment(null);
            review.setReviewedAt(null);
            reviewMapper.updateById(review);
        }

        ActivityAuditLog log = new ActivityAuditLog();
        log.setActivityId(activityId);
        log.setOperatorId(operator.getId());
        log.setAction(AuditAction.SUBMIT);
        log.setComment(request.getComment());
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
        return activity;
    }

    /**
     * 查询组织者活动列表。
     *
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @param keyword 关键字筛选
     * @param startFrom 开始时间下界
     * @param startTo 开始时间上界
     * @return 活动列表
     */
    @Override
    public List<ActivityListItemView> listOwnActivities(
            Long operatorUserId,
            UserRole operatorRole,
            String keyword,
            LocalDateTime startFrom,
            LocalDateTime startTo
    ) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);
        return activityMapper.selectActivityList(null, null, operator.getId(), keyword, startFrom, startTo);
    }

    /**
     * 查询活动报名用户列表。
     *
     * @param activityId 活动 ID
     * @param status 报名状态筛选
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 报名用户列表
     */
    @Override
    public List<ActivityRegistrationUserView> listActivityRegistrations(
            Long activityId,
            RegistrationStatus status,
            Long operatorUserId,
            UserRole operatorRole
    ) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);
        getOwnedActivityOrThrow(activityId, operator.getId());

        QueryWrapper<ActivityRegistration> wrapper = new QueryWrapper<ActivityRegistration>()
                .eq("activity_id", activityId)
                .orderByDesc("registered_at", "id");
        if (status != null) {
            wrapper.eq("status", status.name());
        }
        List<ActivityRegistration> registrations = registrationMapper.selectList(wrapper);
        if (registrations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = registrations.stream()
                .map(ActivityRegistration::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<User> users = userIds.isEmpty() ? Collections.emptyList() : userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<ActivityRegistrationUserView> result = new ArrayList<>(registrations.size());
        for (ActivityRegistration registration : registrations) {
            ActivityRegistrationUserView view = new ActivityRegistrationUserView();
            view.setRegistrationId(registration.getId());
            view.setUserId(registration.getUserId());
            view.setStatus(registration.getStatus());
            view.setRemark(registration.getRemark());
            view.setRegisteredAt(registration.getRegisteredAt());
            view.setCancelledAt(registration.getCancelledAt());
            view.setCheckinAt(registration.getCheckinAt());
            User user = userMap.get(registration.getUserId());
            if (user != null) {
                view.setNickname(user.getNickname());
                view.setPhone(user.getPhone());
            }
            result.add(view);
        }
        return result;
    }

    /**
     * 组织者按票码签到。
     */
    @Override
    @Transactional
    public CheckinResultView checkInByTicketCode(
            Long activityId,
            OrganizerActivityCheckinRequest request,
            Long operatorUserId,
            UserRole operatorRole
    ) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);
        getOwnedActivityOrThrow(activityId, operator.getId());

        ActivityRegistration registration = registrationMapper.selectOne(
                new QueryWrapper<ActivityRegistration>()
                        .eq("ticket_code", request.getTicketCode())
                        .last("LIMIT 1")
        );
        if (registration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "ticket not found");
        }
        if (!activityId.equals(registration.getActivityId())) {
            throw new BusinessException(ErrorCode.CONFLICT, "ticket does not belong to this activity");
        }
        if (RegistrationStatus.CANCELLED.equals(registration.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "cancelled ticket cannot check in");
        }
        if (RegistrationStatus.CHECKED_IN.equals(registration.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "ticket already checked in");
        }
        if (!RegistrationStatus.REGISTERED.equals(registration.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "registration status cannot check in");
        }

        registration.setStatus(RegistrationStatus.CHECKED_IN);
        registration.setCheckinAt(LocalDateTime.now());
        registration.setCheckinOperatorId(operator.getId());
        registrationMapper.updateById(registration);

        User attendee = userMapper.selectById(registration.getUserId());
        CheckinResultView result = new CheckinResultView();
        result.setRegistrationId(registration.getId());
        result.setActivityId(registration.getActivityId());
        result.setUserId(registration.getUserId());
        result.setNickname(attendee != null ? attendee.getNickname() : null);
        result.setStatus(registration.getStatus());
        result.setCheckedInAt(registration.getCheckinAt());
        result.setOperatorUserId(operator.getId());
        return result;
    }

    /**
     * 获取组织者名下活动。
     * 统一校验活动存在且归属当前组织者。
     */
    private Activity getOwnedActivityOrThrow(Long activityId, Long organizerId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + activityId);
        }
        if (!organizerId.equals(activity.getOrganizerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "organizer cannot operate another organizer's activity");
        }
        return activity;
    }

    /**
     * 判断更新请求中是否至少包含一个可更新字段。
     */
    private boolean hasAnyUpdatableField(OrganizerActivityUpdateRequest request) {
        return StringUtils.hasText(request.getTitle())
                || StringUtils.hasText(request.getSummary())
                || request.getContent() != null
                || request.getCoverUrl() != null
                || StringUtils.hasText(request.getLocation())
                || request.getStartTime() != null
                || request.getEndTime() != null
                || request.getRegistrationDeadline() != null
                || request.getMaxParticipants() != null
                || request.getVisibility() != null
                || request.getFeatured() != null;
    }

    /**
     * 校验活动时间窗口。
     * 规则：start/end 必填、end 必须晚于 start、报名截止时间不得晚于 start。
     */
    private void validateActivityTime(LocalDateTime start, LocalDateTime end, LocalDateTime registrationDeadline) {
        if (start == null || end == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "startTime and endTime are required");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "endTime must be later than startTime");
        }
        if (registrationDeadline != null && registrationDeadline.isAfter(start)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "registrationDeadline must be <= startTime");
        }
    }
}
