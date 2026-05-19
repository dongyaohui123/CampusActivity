package com.campus.activity.controller.v1;

import com.campus.activity.common.ApiResponse;
import com.campus.activity.dto.v1.review.ReviewApproveRequest;
import com.campus.activity.dto.v1.review.ReviewRejectRequest;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.enums.UserRole;
import com.campus.activity.service.v1.V1AdminReviewService;
import com.campus.activity.view.v1.PendingReviewActivityView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员审核控制器（v1）。
 * 提供待审核列表查询、通过、驳回能力。
 */
@Validated
@RestController
@RequestMapping("/api/v1/admin/reviews")
public class V1AdminReviewController {
    private final V1AdminReviewService adminReviewService;

    /**
     * 构造函数。
     *
     * @param adminReviewService 审核服务
     */
    public V1AdminReviewController(V1AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    /**
     * 查询待审核活动列表。
     *
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 待审核活动列表
     */
    @GetMapping("/pending")
    public ApiResponse<List<PendingReviewActivityView>> listPendingReviews(
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success(adminReviewService.listPendingReviews(operatorUserId, operatorRole));
    }

    /**
     * 审核通过活动。
     *
     * @param activityId 活动 ID
     * @param request 审核通过请求（可空）
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 最新审核记录
     */
    @PostMapping("/{activityId}/approve")
    public ApiResponse<ActivityReview> approve(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId,
            @Valid @RequestBody(required = false) ReviewApproveRequest request,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        ReviewApproveRequest safeRequest = request == null ? new ReviewApproveRequest() : request;
        return ApiResponse.success("review approved",
                adminReviewService.approve(activityId, safeRequest, operatorUserId, operatorRole));
    }

    /**
     * 审核驳回活动。
     *
     * @param activityId 活动 ID
     * @param request 审核驳回请求
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 最新审核记录
     */
    @PostMapping("/{activityId}/reject")
    public ApiResponse<ActivityReview> reject(
            @PathVariable("activityId") @Min(value = 1, message = "activityId must be >= 1") Long activityId,
            @Valid @RequestBody ReviewRejectRequest request,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success("review rejected",
                adminReviewService.reject(activityId, request, operatorUserId, operatorRole));
    }
}
