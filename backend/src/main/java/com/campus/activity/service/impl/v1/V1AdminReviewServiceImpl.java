package com.campus.activity.service.impl.v1;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.review.ReviewApproveRequest;
import com.campus.activity.dto.v1.review.ReviewRejectRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityAuditLog;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.User;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.AuditAction;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityAuditLogMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.service.v1.V1AdminReviewService;
import com.campus.activity.view.v1.PendingReviewActivityView;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理员审核服务实现（v1）。
 * 负责待审核列表、审核通过、审核驳回与审计日志写入。
 */
@Service
public class V1AdminReviewServiceImpl implements V1AdminReviewService {
    private final ActivityReviewMapper reviewMapper;
    private final ActivityMapper activityMapper;
    private final ActivityAuditLogMapper auditLogMapper;
    private final UserMapper userMapper;
    private final OperatorPermissionService permissionService;

    /**
     * 构造函数。
     */
    public V1AdminReviewServiceImpl(
            ActivityReviewMapper reviewMapper,
            ActivityMapper activityMapper,
            ActivityAuditLogMapper auditLogMapper,
            UserMapper userMapper,
            OperatorPermissionService permissionService
    ) {
        this.reviewMapper = reviewMapper;
        this.activityMapper = activityMapper;
        this.auditLogMapper = auditLogMapper;
        this.userMapper = userMapper;
        this.permissionService = permissionService;
    }

    /**
     * 查询待审核活动列表。
     *
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 待审核活动列表
     */
    @Override
    public List<PendingReviewActivityView> listPendingReviews(Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ADMIN);

        List<ActivityReview> pendingReviews = reviewMapper.selectList(
                new QueryWrapper<ActivityReview>()
                        .eq("review_status", ReviewStatus.PENDING.name())
                        .orderByDesc("updated_at", "created_at", "activity_id")
        );
        if (pendingReviews.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> activityIds = pendingReviews.stream()
                .map(ActivityReview::getActivityId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Activity> activityMap = activityIds.isEmpty()
                ? Collections.emptyMap()
                : activityMapper.selectBatchIds(activityIds).stream()
                        .collect(Collectors.toMap(Activity::getId, a -> a));
        List<Long> organizerIds = activityMap.values().stream()
                .map(Activity::getOrganizerId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, User> organizerMap = organizerIds.isEmpty()
                ? Collections.emptyMap()
                : userMapper.selectBatchIds(organizerIds).stream()
                        .collect(Collectors.toMap(User::getId, u -> u));

        return pendingReviews.stream()
                .map(review -> {
                    Activity activity = activityMap.get(review.getActivityId());
                    if (activity == null) {
                        return null;
                    }
                    PendingReviewActivityView view = new PendingReviewActivityView();
                    view.setActivityId(activity.getId());
                    view.setTitle(activity.getTitle());
                    view.setOrganizerId(activity.getOrganizerId());
                    view.setOrganizerName(resolveDisplayName(organizerMap.get(activity.getOrganizerId())));
                    view.setStartTime(activity.getStartTime());
                    view.setEndTime(activity.getEndTime());
                    view.setReviewStatus(review.getReviewStatus());
                    view.setSubmittedAt(review.getUpdatedAt() != null ? review.getUpdatedAt() : review.getCreatedAt());
                    return view;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String resolveDisplayName(User organizer) {
        if (organizer == null) {
            return "-";
        }
        String nickname = organizer.getNickname();
        if (nickname != null && !nickname.isBlank()) {
            return nickname.trim();
        }
        String username = organizer.getUsername();
        if (username != null && !username.isBlank()) {
            return username.trim();
        }
        return "-";
    }

    /**
     * 审核通过活动。
     *
     * @param activityId 活动 ID
     * @param request 审核通过请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 审核记录
     */
    @Override
    @Transactional
    public ActivityReview approve(Long activityId, ReviewApproveRequest request, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ADMIN);

        Activity activity = getActivityOrThrow(activityId);
        ActivityReview review = getPendingReviewOrThrow(activityId);
        LocalDateTime now = LocalDateTime.now();

        review.setReviewStatus(ReviewStatus.APPROVED);
        review.setReviewerId(operator.getId());
        review.setReviewComment(request != null ? request.getComment() : null);
        review.setReviewedAt(now);
        reviewMapper.updateById(review);

        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setFeatured(request != null && Boolean.TRUE.equals(request.getFeatured()));
        if (activity.getPublishedAt() == null) {
            activity.setPublishedAt(now);
        }
        activityMapper.updateById(activity);

        insertAuditLog(activityId, operator.getId(), AuditAction.APPROVE, request != null ? request.getComment() : null, now);
        return review;
    }

    /**
     * 审核驳回活动。
     *
     * @param activityId 活动 ID
     * @param request 审核驳回请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 审核记录
     */
    @Override
    @Transactional
    public ActivityReview reject(Long activityId, ReviewRejectRequest request, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ADMIN);

        Activity activity = getActivityOrThrow(activityId);
        ActivityReview review = getPendingReviewOrThrow(activityId);
        LocalDateTime now = LocalDateTime.now();

        review.setReviewStatus(ReviewStatus.REJECTED);
        review.setReviewerId(operator.getId());
        review.setReviewComment(request.getComment());
        review.setReviewedAt(now);
        reviewMapper.updateById(review);

        activity.setStatus(ActivityStatus.DRAFT);
        activityMapper.updateById(activity);

        insertAuditLog(activityId, operator.getId(), AuditAction.REJECT, request.getComment(), now);
        return review;
    }

    /**
     * 获取活动并校验“可审核”状态。
     */
    private Activity getActivityOrThrow(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + activityId);
        }
        if (ActivityStatus.CANCELLED.equals(activity.getStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "cancelled activity cannot be reviewed");
        }
        return activity;
    }

    /**
     * 获取待审核记录并校验状态必须为 PENDING。
     */
    private ActivityReview getPendingReviewOrThrow(Long activityId) {
        ActivityReview review = reviewMapper.selectById(activityId);
        if (review == null) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity has not been submitted for review");
        }
        if (!ReviewStatus.PENDING.equals(review.getReviewStatus())) {
            throw new BusinessException(ErrorCode.CONFLICT, "activity review is not in pending status");
        }
        return review;
    }

    /**
     * 写入审核审计日志。
     */
    private void insertAuditLog(Long activityId, Long operatorId, AuditAction action, String comment, LocalDateTime createdAt) {
        ActivityAuditLog log = new ActivityAuditLog();
        log.setActivityId(activityId);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setComment(comment);
        log.setCreatedAt(createdAt);
        auditLogMapper.insert(log);
    }
}
