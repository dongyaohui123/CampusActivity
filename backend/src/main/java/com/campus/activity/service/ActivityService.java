package com.campus.activity.service;

import com.campus.activity.dto.activity.CancelActivityRequest;
import com.campus.activity.dto.activity.CreateActivityRequest;
import com.campus.activity.dto.activity.UpdateActivityRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.Visibility;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 活动管理服务（旧版接口）。
 */
public interface ActivityService {
    /**
     * 创建活动。
     *
     * @param request 创建请求参数
     * @return 创建后的活动
     */
    Activity createActivity(CreateActivityRequest request);

    /**
     * 更新活动。
     *
     * @param id 活动 ID
     * @param request 更新请求参数
     * @return 更新后的活动
     */
    Activity updateActivity(Long id, UpdateActivityRequest request);

    /**
     * 取消活动。
     *
     * @param id 活动 ID
     * @param request 取消请求参数
     */
    void cancelActivity(Long id, CancelActivityRequest request);

    /**
     * 按 ID 查询活动详情。
     *
     * @param id 活动 ID
     * @return 活动详情
     */
    Activity getActivityById(Long id);

    /**
     * 按条件查询活动列表。
     *
     * @param status 活动状态筛选
     * @param visibility 可见性筛选
     * @param organizerId 主办方用户 ID 筛选
     * @param keyword 关键字筛选
     * @param startFrom 活动开始时间下界
     * @param startTo 活动开始时间上界
     * @return 活动列表
     */
    List<ActivityListItemView> listActivities(
            ActivityStatus status,
            Visibility visibility,
            Long organizerId,
            String keyword,
            LocalDateTime startFrom,
            LocalDateTime startTo
    );
}
