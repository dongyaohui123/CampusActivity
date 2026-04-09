package com.campus.activity.service.v1;

import com.campus.activity.dto.v1.review.ReviewApproveRequest;
import com.campus.activity.dto.v1.review.ReviewRejectRequest;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.enums.UserRole;
import com.campus.activity.view.v1.PendingReviewActivityView;
import java.util.List;

public interface V1AdminReviewService {
    List<PendingReviewActivityView> listPendingReviews(Long operatorUserId, UserRole operatorRole);

    ActivityReview approve(Long activityId, ReviewApproveRequest request, Long operatorUserId, UserRole operatorRole);

    ActivityReview reject(Long activityId, ReviewRejectRequest request, Long operatorUserId, UserRole operatorRole);
}
