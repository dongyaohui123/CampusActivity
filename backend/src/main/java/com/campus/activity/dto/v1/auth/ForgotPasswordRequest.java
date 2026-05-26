package com.campus.activity.dto.v1.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ForgotPasswordRequest {
    @NotBlank(message = "username is required")
    @Size(max = 64, message = "username length must be <= 64")
    private String username;

    @NotBlank(message = "phone is required")
    @Size(max = 20, message = "phone length must be <= 20")
    @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
    private String phone;

    @NotBlank(message = "newPassword is required")
    @Size(max = 255, message = "newPassword length must be <= 255")
    private String newPassword;

    @NotBlank(message = "code is required")
    @Size(max = 10, message = "code length must be <= 10")
    private String code;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
