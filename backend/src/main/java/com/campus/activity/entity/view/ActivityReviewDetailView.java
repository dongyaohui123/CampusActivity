package com.campus.activity.entity.view;

import com.campus.activity.enums.ReviewStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * Review details enriched with reviewer basic information.
 */
@Data
public class ActivityReviewDetailView {

    private Long activityId;

    private ReviewStatus reviewStatus;

    private Long reviewerId;

    private String reviewerNickname;

    private String reviewComment;

    private LocalDateTime reviewedAt;
}
