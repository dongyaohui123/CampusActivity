package com.campus.activity.service.impl.v1;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.Visibility;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.service.v1.V1PublicActivityService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * V1PublicActivityServiceImpl服务实现。
 */
@Service
public class V1PublicActivityServiceImpl implements V1PublicActivityService {
    private static final Set<ActivityStatus> PUBLIC_STATUS = Set.of(
            ActivityStatus.PUBLISHED,
            ActivityStatus.REGISTRATION_OPEN,
            ActivityStatus.REGISTRATION_CLOSED,
            ActivityStatus.ONGOING,
            ActivityStatus.FINISHED
    );

    private final ActivityMapper activityMapper;
    private final ActivityReviewMapper reviewMapper;

    /**
     * 构造函数。
     *
     * @param activityMapper 活动数据访问
     * @param reviewMapper 审核数据访问
     */
    public V1PublicActivityServiceImpl(ActivityMapper activityMapper, ActivityReviewMapper reviewMapper) {
        this.activityMapper = activityMapper;
        this.reviewMapper = reviewMapper;
    }

    /**
     * 查询公开活动列表。
     *
     * @param keyword 关键字筛选
     * @param startFrom 活动开始时间下界
     * @param startTo 活动开始时间上界
     * @return 符合公开条件的活动列表
     */
    @Override
    public List<ActivityListItemView> listPublicActivities(String keyword, LocalDateTime startFrom, LocalDateTime startTo) {
        List<ActivityListItemView> raw = activityMapper.selectActivityList(
                null,
                Visibility.PUBLIC,
                null,
                keyword,
                startFrom,
                startTo
        );
        return raw.stream()
                .filter(item -> item.getReviewStatus() == ReviewStatus.APPROVED)
                .filter(item -> item.getStatus() != null && PUBLIC_STATUS.contains(item.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 查询公开活动详情。
     *
     * @param activityId 活动 ID
     * @return 活动详情
     */
    @Override
    public Activity getPublicActivityDetail(Long activityId) {
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
        return activity;
    }
}
