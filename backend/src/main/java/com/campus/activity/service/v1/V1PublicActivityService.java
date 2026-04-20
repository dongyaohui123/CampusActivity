package com.campus.activity.service.v1;

import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 公开活动查询服务（v1）。
 */
public interface V1PublicActivityService {
    /**
     * 查询公开活动列表。
     *
     * @param keyword 关键字筛选
     * @param startFrom 活动开始时间下界
     * @param startTo 活动开始时间上界
     * @return 公开活动列表
     */
    List<ActivityListItemView> listPublicActivities(String keyword, LocalDateTime startFrom, LocalDateTime startTo);

    /**
     * 查询公开活动详情。
     *
     * @param activityId 活动 ID
     * @return 活动详情
     */
    Activity getPublicActivityDetail(Long activityId);
}
