package com.campus.activity.service.v1;

import com.campus.activity.dto.v1.activity.OrganizerActivityCreateRequest;
import com.campus.activity.dto.v1.activity.OrganizerActivityUpdateRequest;
import com.campus.activity.dto.v1.activity.SubmitReviewRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.RegistrationStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.view.v1.ActivityRegistrationUserView;
import java.time.LocalDateTime;
import java.util.List;

/**
 * V1OrganizerActivityService服务接口。
 */
public interface V1OrganizerActivityService {
    /**
     * 主办方创建活动。
     *
     * @param request 创建请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 活动信息
     */
    Activity createActivity(OrganizerActivityCreateRequest request, Long operatorUserId, UserRole operatorRole);

    /**
     * 主办方更新活动。
     *
     * @param activityId 活动 ID
     * @param request 更新请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 活动信息
     */
    Activity updateActivity(Long activityId, OrganizerActivityUpdateRequest request, Long operatorUserId, UserRole operatorRole);

    /**
     * 提交活动审核。
     *
     * @param activityId 活动 ID
     * @param request 提交审核请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 活动信息
     */
    Activity submitForReview(Long activityId, SubmitReviewRequest request, Long operatorUserId, UserRole operatorRole);

    /**
     * 查询主办方活动列表。
     *
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @param keyword 关键字筛选
     * @param startFrom 活动开始时间下界
     * @param startTo 活动开始时间上界
     * @return 活动列表
     */
    List<ActivityListItemView> listOwnActivities(Long operatorUserId, UserRole operatorRole, String keyword, LocalDateTime startFrom, LocalDateTime startTo);

    /**
     * 查询活动报名用户列表。
     *
     * @param activityId 活动 ID
     * @param status 报名状态筛选
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 报名用户列表
     */
    List<ActivityRegistrationUserView> listActivityRegistrations(Long activityId, RegistrationStatus status, Long operatorUserId, UserRole operatorRole);
}
