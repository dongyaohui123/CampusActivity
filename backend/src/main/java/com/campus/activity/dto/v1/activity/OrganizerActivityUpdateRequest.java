package com.campus.activity.dto.v1.activity;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * OrganizerActivityUpdateRequest请求参数对象。
 */
public class OrganizerActivityUpdateRequest {
    @Size(max = 200, message = "title length must be <= 200")
    private String title;
    @Size(max = 500, message = "summary length must be <= 500")
    private String summary;
    private String content;
    @Size(max = 255, message = "coverUrl length must be <= 255")
    private String coverUrl;
    @Size(max = 255, message = "location length must be <= 255")
    private String location;
    @Pattern(regexp = "^(SOUTH|NORTH|ONLINE)$", message = "campusCode must be SOUTH/NORTH/ONLINE")
    private String campusCode;
    @Min(value = 1, message = "activityTypeId must be >= 1")
    private Long activityTypeId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime registrationDeadline;
    @Min(value = 0, message = "maxParticipants must be >= 0")
    private Integer maxParticipants;
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCampusCode() { return campusCode; }
    public void setCampusCode(String campusCode) { this.campusCode = campusCode; }
    public Long getActivityTypeId() { return activityTypeId; }
    public void setActivityTypeId(Long activityTypeId) { this.activityTypeId = activityTypeId; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public LocalDateTime getRegistrationDeadline() { return registrationDeadline; }
    public void setRegistrationDeadline(LocalDateTime registrationDeadline) { this.registrationDeadline = registrationDeadline; }
    public Integer getMaxParticipants() { return maxParticipants; }
    public void setMaxParticipants(Integer maxParticipants) { this.maxParticipants = maxParticipants; }
}
