package com.campus.activity.dto.user;

import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Update user payload with whitelist fields only.
 */
public class UpdateUserRequest {
    @Size(max = 64, message = "username length must be <= 64")
    private String username;

    @Size(max = 255, message = "passwordHash length must be <= 255")
    private String passwordHash;

    @Size(max = 64, message = "openid length must be <= 64")
    private String openid;

    @Size(max = 100, message = "nickname length must be <= 100")
    private String nickname;

    @Size(max = 255, message = "avatarUrl length must be <= 255")
    private String avatarUrl;

    @Min(value = 0, message = "gender must be 0/1/2")
    @Max(value = 2, message = "gender must be 0/1/2")
    private Integer gender;

    @Size(max = 20, message = "phone length must be <= 20")
    private String phone;

    private UserRole role;

    private UserStatus status;

    private Boolean forcePasswordChange;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getOpenid() {
        return openid;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public Boolean getForcePasswordChange() {
        return forcePasswordChange;
    }

    public void setForcePasswordChange(Boolean forcePasswordChange) {
        this.forcePasswordChange = forcePasswordChange;
    }
}
