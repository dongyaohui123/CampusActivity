package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.view.ActivityReviewDetailView;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for activity_reviews.
 */
public interface ActivityReviewMapper extends BaseMapper<ActivityReview> {

    /**
     * Query one review row and reviewer nickname by activity id.
     */
    ActivityReviewDetailView selectDetailByActivityId(@Param("activityId") Long activityId);
}
