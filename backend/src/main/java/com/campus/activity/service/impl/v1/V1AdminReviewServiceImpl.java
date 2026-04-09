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

@Service
public class V1AdminReviewServiceImpl implements V1AdminReviewService {
    private final ActivityReviewMapper reviewMapper;
    private final ActivityMapper activityMapper;
    private final ActivityAuditLogMapper auditLogMapper;
    private final OperatorPermissionService permissionService;

    public V1AdminReviewServiceImpl(
            ActivityReviewMapper reviewMapper,
            ActivityMapper activityMapper,
            ActivityAuditLogMapper auditLogMapper,
            OperatorPermissionService permissionService
    ) {
        this.reviewMapper = reviewMapper;
        this.activityMapper = activityMapper;
        this.auditLogMapper = auditLogMapper;
        this.permissionService = permissionService;
    }

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
                    view.setStartTime(activity.getStartTime());
                    view.setEndTime(activity.getEndTime());
                    view.setReviewStatus(review.getReviewStatus());
                    view.setSubmittedAt(review.getUpdatedAt() != null ? review.getUpdatedAt() : review.getCreatedAt());
                    return view;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

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
        if (activity.getPublishedAt() == null) {
            activity.setPublishedAt(now);
        }
        activityMapper.updateById(activity);

        insertAuditLog(activityId, operator.getId(), AuditAction.APPROVE, request != null ? request.getComment() : null, now);
        return review;
    }

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
