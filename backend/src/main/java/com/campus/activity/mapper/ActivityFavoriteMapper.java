package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.ActivityFavorite;
import org.apache.ibatis.annotations.Param;

/**
 * Mapper for activity_favorites.
 */
public interface ActivityFavoriteMapper extends BaseMapper<ActivityFavorite> {

    /**
     * Check whether user has favorited the activity.
     */
    Boolean existsFavorite(@Param("activityId") Long activityId,
                           @Param("userId") Long userId);
}
