package com.campus.activity.service.impl.v1;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityFavorite;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.User;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.Visibility;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityFavoriteMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.service.v1.V1ActivityFavoriteService;
import com.campus.activity.view.v1.ActivityFavoriteStateView;
import com.campus.activity.view.v1.FavoriteActivityView;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 活动收藏服务实现。
 */
@Service
public class V1ActivityFavoriteServiceImpl implements V1ActivityFavoriteService {
    private static final Set<ActivityStatus> PUBLIC_STATUS = Set.of(
            ActivityStatus.PUBLISHED,
            ActivityStatus.REGISTRATION_OPEN,
            ActivityStatus.REGISTRATION_CLOSED,
            ActivityStatus.ONGOING,
            ActivityStatus.FINISHED
    );

    private final ActivityMapper activityMapper;
    private final ActivityReviewMapper reviewMapper;
    private final ActivityFavoriteMapper favoriteMapper;
    private final OperatorPermissionService permissionService;

    public V1ActivityFavoriteServiceImpl(
            ActivityMapper activityMapper,
            ActivityReviewMapper reviewMapper,
            ActivityFavoriteMapper favoriteMapper,
            OperatorPermissionService permissionService
    ) {
        this.activityMapper = activityMapper;
        this.reviewMapper = reviewMapper;
        this.favoriteMapper = favoriteMapper;
        this.permissionService = permissionService;
    }

    @Override
    public ActivityFavoriteStateView getFavoriteState(Long activityId, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        ensurePublicApprovedActivity(activityId);
        return buildState(Boolean.TRUE.equals(favoriteMapper.existsFavorite(activityId, operator.getId())));
    }

    @Override
    @Transactional
    public ActivityFavoriteStateView favoriteActivity(Long activityId, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        ensurePublicApprovedActivity(activityId);

        ActivityFavorite existing = favoriteMapper.selectOne(
                new QueryWrapper<ActivityFavorite>()
                        .eq("activity_id", activityId)
                        .eq("user_id", operator.getId())
                        .last("LIMIT 1")
        );
        if (existing == null) {
            ActivityFavorite favorite = new ActivityFavorite();
            favorite.setActivityId(activityId);
            favorite.setUserId(operator.getId());
            favorite.setCreatedAt(LocalDateTime.now());
            favoriteMapper.insert(favorite);
        }
        return buildState(true);
    }

    @Override
    @Transactional
    public ActivityFavoriteStateView unfavoriteActivity(Long activityId, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        ensurePublicApprovedActivity(activityId);

        favoriteMapper.delete(new QueryWrapper<ActivityFavorite>()
                .eq("activity_id", activityId)
                .eq("user_id", operator.getId()));
        return buildState(false);
    }

    @Override
    public List<FavoriteActivityView> getUserFavorites(Long userId, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireSelfOrAdmin(operator, userId);
        return favoriteMapper.selectUserFavoriteActivities(userId);
    }

    private ActivityFavoriteStateView buildState(boolean favorited) {
        ActivityFavoriteStateView view = new ActivityFavoriteStateView();
        view.setFavorited(favorited);
        return view;
    }

    private void ensurePublicApprovedActivity(Long activityId) {
        Activity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity not found: " + activityId);
        }
        if (!Visibility.PUBLIC.equals(activity.getVisibility()) || !PUBLIC_STATUS.contains(activity.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity is not publicly visible");
        }

        ActivityReview review = reviewMapper.selectById(activityId);
        if (review == null || !ReviewStatus.APPROVED.equals(review.getReviewStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "activity is not approved yet");
        }
    }
}
