package com.campus.activity.service.v1;

import com.campus.activity.enums.UserRole;
import com.campus.activity.view.v1.RecommendActivityView;
import java.util.List;

/**
 * 推荐服务接口（v1）。
 */
public interface V1RecommendationService {

    /**
     * 获取个性化推荐活动列表。
     *
     * @param operatorUserId 操作人用户 ID
     * @param operatorRole 操作人角色
     * @param limit 返回数量限制
     * @return 推荐活动列表
     */
    List<RecommendActivityView> getRecommendedActivities(Long operatorUserId, UserRole operatorRole, int limit);
}
