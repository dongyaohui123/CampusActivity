package com.campus.activity.service.v1;

import com.campus.activity.dto.v1.review.ReviewApproveRequest;
import com.campus.activity.dto.v1.review.ReviewRejectRequest;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.enums.UserRole;
import com.campus.activity.view.v1.PendingReviewActivityView;
import java.util.List;

/**
 * V1AdminReviewService服务接口。
 */
public interface V1AdminReviewService {
    /**
     * 查询待审核活动列表。
     *
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 待审核活动列表
     */
    List<PendingReviewActivityView> listPendingReviews(Long operatorUserId, UserRole operatorRole);

    /**
     * 审核通过活动。
     *
     * @param activityId 活动 ID
     * @param request 审核通过请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 审核记录
     */
    ActivityReview approve(Long activityId, ReviewApproveRequest request, Long operatorUserId, UserRole operatorRole);

    /**
     * 审核驳回活动。
     *
     * @param activityId 活动 ID
     * @param request 审核驳回请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 审核记录
     */
    ActivityReview reject(Long activityId, ReviewRejectRequest request, Long operatorUserId, UserRole operatorRole);
}
