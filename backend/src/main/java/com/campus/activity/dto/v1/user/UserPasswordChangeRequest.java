package com.campus.activity.dto.v1.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * UserPasswordChangeRequest请求参数对象。
 */
public class UserPasswordChangeRequest {
    @NotBlank(message = "oldPassword is required")
    @Size(max = 255, message = "oldPassword length must be <= 255")
    private String oldPassword;

    @NotBlank(message = "newPassword is required")
    @Size(max = 255, message = "newPassword length must be <= 255")
    private String newPassword;

    public String getOldPassword() {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
