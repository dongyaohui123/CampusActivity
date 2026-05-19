package com.campus.activity.service.v1;

import com.campus.activity.enums.UserRole;
import com.campus.activity.view.v1.ActivityFavoriteStateView;
import com.campus.activity.view.v1.FavoriteActivityView;
import java.util.List;

/**
 * 活动收藏服务接口。
 */
public interface V1ActivityFavoriteService {
    /**
     * 查询当前操作人的活动收藏状态。
     */
    ActivityFavoriteStateView getFavoriteState(Long activityId, Long operatorUserId, UserRole operatorRole);

    /**
     * 收藏活动。
     */
    ActivityFavoriteStateView favoriteActivity(Long activityId, Long operatorUserId, UserRole operatorRole);

    /**
     * 取消收藏活动。
     */
    ActivityFavoriteStateView unfavoriteActivity(Long activityId, Long operatorUserId, UserRole operatorRole);

    /**
     * 查询用户收藏活动列表。
     */
    List<FavoriteActivityView> getUserFavorites(Long userId, Long operatorUserId, UserRole operatorRole);
}
