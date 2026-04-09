package com.campus.activity.dto.v1.registration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RegistrationCreateRequest {
    @NotNull(message = "activityId is required")
    private Long activityId;

    @Size(max = 255, message = "remark length must be <= 255")
    private String remark;

    public Long getActivityId() { return activityId; }
    public void setActivityId(Long activityId) { this.activityId = activityId; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
