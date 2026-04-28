package com.campus.activity.view.v1;

import com.campus.activity.enums.ActivityManagerPermission;
import com.campus.activity.enums.BasicStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Activity manager view.
 */
public class ActivityManagerView {
    private Long userId;
    private String nickname;
    private String phone;
    private List<ActivityManagerPermission> permissions;
    private BasicStatus status;
    private LocalDateTime createdAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public List<ActivityManagerPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<ActivityManagerPermission> permissions) {
        this.permissions = permissions;
    }

    public BasicStatus getStatus() {
        return status;
    }

    public void setStatus(BasicStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
