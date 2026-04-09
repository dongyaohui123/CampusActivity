package com.campus.activity.dto.v1.registration;

import jakarta.validation.constraints.Size;

public class RegistrationCancelRequest {
    @Size(max = 255, message = "remark length must be <= 255")
    private String remark;

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
