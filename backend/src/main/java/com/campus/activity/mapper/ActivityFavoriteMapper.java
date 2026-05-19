package com.campus.activity.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campus.activity.entity.ActivityFavorite;
import com.campus.activity.view.v1.FavoriteActivityView;
import java.util.List;
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

    /**
     * 查询用户收藏活动列表。
     */
    List<FavoriteActivityView> selectUserFavoriteActivities(@Param("userId") Long userId);
}
