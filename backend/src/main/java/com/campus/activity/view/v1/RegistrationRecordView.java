package com.campus.activity.view.v1;

import com.campus.activity.enums.RegistrationStatus;
import java.time.LocalDateTime;

public class RegistrationRecordView {
    private Long registrationId;
    private Long activityId;
    private String activityTitle;
    private String location;
    private LocalDateTime activityStartTime;
    private LocalDateTime activityEndTime;
    private RegistrationStatus registrationStatus;
    private LocalDateTime registeredAt;
    private LocalDateTime cancelledAt;

    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getActivityTitle() { return activityTitle; }
    public void setActivityTitle(String activityTitle) { this.activityTitle = activityTitle; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public LocalDateTime getActivityStartTime() { return activityStartTime; }
    public void setActivityStartTime(LocalDateTime activityStartTime) { this.activityStartTime = activityStartTime; }
    public LocalDateTime getActivityEndTime() { return activityEndTime; }
    public void setActivityEndTime(LocalDateTime activityEndTime) { this.activityEndTime = activityEndTime; }
    public RegistrationStatus getRegistrationStatus() { return registrationStatus; }
    public void setRegistrationStatus(RegistrationStatus registrationStatus) { this.registrationStatus = registrationStatus; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
