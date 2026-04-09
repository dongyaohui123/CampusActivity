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

@Validated
@RestController
@RequestMapping("/api/v1/admin/reviews")
public class V1AdminReviewController {
    private final V1AdminReviewService adminReviewService;

    public V1AdminReviewController(V1AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    @GetMapping("/pending")
    public ApiResponse<List<PendingReviewActivityView>> listPendingReviews(
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success(adminReviewService.listPendingReviews(operatorUserId, operatorRole));
    }

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
