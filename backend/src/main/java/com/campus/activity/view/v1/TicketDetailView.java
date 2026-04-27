package com.campus.activity.view.v1;

import com.campus.activity.enums.RegistrationStatus;
import java.time.LocalDateTime;

/**
 * 电子票详情视图。
 */
public class TicketDetailView {
    private Long registrationId;
    private Long activityId;
    private String activityTitle;
    private String location;
    private LocalDateTime activityStartTime;
    private LocalDateTime activityEndTime;
    private RegistrationStatus registrationStatus;
    private String ticketCode;
    private LocalDateTime ticketIssuedAt;
    private LocalDateTime checkinAt;
    private String qrContent;

    public Long getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(Long registrationId) {
        this.registrationId = registrationId;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getActivityTitle() {
        return activityTitle;
    }

    public void setActivityTitle(String activityTitle) {
        this.activityTitle = activityTitle;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getActivityStartTime() {
        return activityStartTime;
    }

    public void setActivityStartTime(LocalDateTime activityStartTime) {
        this.activityStartTime = activityStartTime;
    }

    public LocalDateTime getActivityEndTime() {
        return activityEndTime;
    }

    public void setActivityEndTime(LocalDateTime activityEndTime) {
        this.activityEndTime = activityEndTime;
    }

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }

    public void setRegistrationStatus(RegistrationStatus registrationStatus) {
        this.registrationStatus = registrationStatus;
    }

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }

    public LocalDateTime getTicketIssuedAt() {
        return ticketIssuedAt;
    }

    public void setTicketIssuedAt(LocalDateTime ticketIssuedAt) {
        this.ticketIssuedAt = ticketIssuedAt;
    }

    public LocalDateTime getCheckinAt() {
        return checkinAt;
    }

    public void setCheckinAt(LocalDateTime checkinAt) {
        this.checkinAt = checkinAt;
    }

    public String getQrContent() {
        return qrContent;
    }

    public void setQrContent(String qrContent) {
        this.qrContent = qrContent;
    }
}
