package com.campus.activity.mapper;

import com.campus.activity.entity.ActivityCategoryRel;
import com.campus.activity.entity.view.ActivityCategoryLinkView;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for activity_category_rel with explicit composite-key operations.
 */
public interface ActivityCategoryRelMapper {

    int insertRelation(ActivityCategoryRel relation);

    int deleteByActivityId(@Param("activityId") Long activityId);

    int deleteByActivityIdAndCategoryId(@Param("activityId") Long activityId,
                                        @Param("categoryId") Long categoryId);

    ActivityCategoryRel selectByActivityIdAndCategoryId(@Param("activityId") Long activityId,
                                                        @Param("categoryId") Long categoryId);

    List<ActivityCategoryLinkView> selectCategoryLinksByActivityId(@Param("activityId") Long activityId);

    List<ActivityCategoryLinkView> selectActivityLinksByCategoryId(@Param("categoryId") Long categoryId);
}
