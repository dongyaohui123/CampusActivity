package com.campus.activity.dto.v1.activity;

import com.campus.activity.enums.ActivityManagerPermission;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request for adding one activity manager.
 */
public class AddActivityManagerRequest {
    @NotBlank(message = "username is required")
    private String username;

    /**
     * Optional permissions.
     * When empty, defaults to VIEW_REGISTRATIONS and CHECK_IN.
     */
    private List<ActivityManagerPermission> permissions;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<ActivityManagerPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<ActivityManagerPermission> permissions) {
        this.permissions = permissions;
    }
}
