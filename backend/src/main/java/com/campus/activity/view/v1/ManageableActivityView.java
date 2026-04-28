package com.campus.activity.view.v1;

import com.campus.activity.enums.ActivityStatus;
import java.time.LocalDateTime;

/**
 * View of activities manageable by current operator.
 */
public class ManageableActivityView {
    private Long id;
    private Long organizerId;
    private String title;
    private String coverUrl;
    private String location;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime registrationDeadline;
    private ActivityStatus status;
    private Boolean canViewRegistrations;
    private Boolean canCheckIn;
    private Boolean isOrganizer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDateTime registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public ActivityStatus getStatus() {
        return status;
    }

    public void setStatus(ActivityStatus status) {
        this.status = status;
    }

    public Boolean getCanViewRegistrations() {
        return canViewRegistrations;
    }

    public void setCanViewRegistrations(Boolean canViewRegistrations) {
        this.canViewRegistrations = canViewRegistrations;
    }

    public Boolean getCanCheckIn() {
        return canCheckIn;
    }

    public void setCanCheckIn(Boolean canCheckIn) {
        this.canCheckIn = canCheckIn;
    }

    public Boolean getIsOrganizer() {
        return isOrganizer;
    }

    public void setIsOrganizer(Boolean isOrganizer) {
        this.isOrganizer = isOrganizer;
    }
}
