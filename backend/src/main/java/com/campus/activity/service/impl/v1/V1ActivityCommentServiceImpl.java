package com.campus.activity.service.impl.v1;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.activity.CreateActivityCommentRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityComment;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.User;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityCommentMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.service.v1.V1ActivityCommentService;
import com.campus.activity.view.v1.ActivityCommentView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Activity comment service implementation.
 */
@Service
public class V1ActivityCommentServiceImpl implements V1ActivityCommentService {
    private static final int COMMENT_MAX_LENGTH = 500;
    private static final Set<ActivityStatus> PUBLIC_STATUS = Set.of(
            ActivityStatus.PUBLISHED,
            ActivityStatus.REGISTRATION_OPEN,
            ActivityStatus.REGISTRATION_CLOSED,
            ActivityStatus.ONGOING,
            ActivityStatus.FINISHED
    );

    private final ActivityMapper activityMapper;
    private final ActivityReviewMapper reviewMapper;
    private final ActivityCommentMapper commentMapper;
    private final OperatorPermissionService permissionService;

    public V1ActivityCommentServiceImpl(ActivityMapper activityMapper,
                                        ActivityReviewMapper reviewMapper,
                                        ActivityCommentMapper commentMapper,
                                        OperatorPermissionService permissionService) {
        this.activityMapper = activityMapper;
        this.reviewMapper = reviewMapper;
        this.commentMapper = commentMapper;
        this.permissionService = permissionService;
    }

    @Override
    public List<ActivityCommentView> listActivityComments(Long activityId) {
        ensurePublicApprovedActivity(activityId);
        return commentMapper.selectActivityComments(activityId);
    }

    @Override
    @Transactional
    public ActivityCommentView createComment(Long activityId,
                                             CreateActivityCommentRequest request,
                                             Long operatorUserId,
                                             UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        ensurePublicApprovedActivity(activityId);

        String normalizedContent = normalizeContent(request == null ? null : request.getContent());
        LocalDateTime now = LocalDateTime.now();

        ActivityComment comment = new ActivityComment();
        comment.setActivityId(activityId);
        comment.setUserId(operator.getId());
        comment.setContent(normalizedContent);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        commentMapper.insert(comment);

        ActivityCommentView view = new ActivityCommentView();
        view.setCommentId(comment.getId());
        view.setActivityId(activityId);
        view.setAuthorUserId(operator.getId());
        view.setAuthorNickname(resolveAuthorNickname(operator));
        view.setAuthorAvatarUrl(operator.getAvatarUrl());
        view.setContent(normalizedContent);
        view.setCreatedAt(now);
        return view;
    }

    @Override
    @Transactional
    public void deleteComment(Long activityId, Long commentId, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        ensurePublicApprovedActivity(activityId);

        ActivityComment comment = commentMapper.selectById(commentId);
        if (comment == null || !activityId.equals(comment.getActivityId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "comment not found");
        }
        permissionService.requireSelf(operator, comment.getUserId());
        commentMapper.delete(new QueryWrapper<ActivityComment>()
                .eq("id", commentId)
                .eq("activity_id", activityId));
    }

    private String normalizeContent(String rawContent) {
        String normalized = rawContent == null ? "" : rawContent.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "comment content must not be blank");
        }
        if (normalized.length() > COMMENT_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "comment content length must be <= 500");
        }
        return normalized;
    }

    private String resolveAuthorNickname(User operator) {
        String nickname = operator == null || operator.getNickname() == null ? "" : operator.getNickname().trim();
        if (!nickname.isEmpty()) {
            return nickname;
        }
        String username = operator == null || operator.getUsername() == null ? "" : operator.getUsername().trim();
        if (!username.isEmpty()) {
            return username;
        }
        return "用户" + (operator == null ? "" : operator.getId());
    }

    private void ensurePublicApprovedActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + activityId);
        }
        if (!PUBLIC_STATUS.contains(activity.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity is not publicly available");
        }

        ActivityReview review = reviewMapper.selectById(activityId);
        if (review == null || !ReviewStatus.APPROVED.equals(review.getReviewStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity is not approved yet");
        }
    }
}
