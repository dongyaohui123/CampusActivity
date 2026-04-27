package com.campus.activity.view.v1;

import com.campus.activity.enums.ReviewStatus;
import java.time.LocalDateTime;

/**
 * PendingReviewActivityView页面视图对象。
 */
public class PendingReviewActivityView {
    private Long activityId;
    private String title;
    private Long organizerId;
    private String organizerName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private ReviewStatus reviewStatus;
    private LocalDateTime submittedAt;

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Long getOrganizerId() { return organizerId; }
    public void setOrganizerId(Long organizerId) { this.organizerId = organizerId; }
    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public ReviewStatus getReviewStatus() { return reviewStatus; }
    public void setReviewStatus(ReviewStatus reviewStatus) { this.reviewStatus = reviewStatus; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
}
