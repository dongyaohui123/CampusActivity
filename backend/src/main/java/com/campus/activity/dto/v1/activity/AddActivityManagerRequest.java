package com.campus.activity.dto.v1.activity;

import com.campus.activity.enums.ActivityManagerPermission;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request for adding one activity manager.
 */
public class AddActivityManagerRequest {
    @NotNull(message = "userId is required")
    @Min(value = 1, message = "userId must be >= 1")
    private Long userId;

    /**
     * Optional permissions.
     * When empty, defaults to VIEW_REGISTRATIONS and CHECK_IN.
     */
    private List<ActivityManagerPermission> permissions;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<ActivityManagerPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<ActivityManagerPermission> permissions) {
        this.permissions = permissions;
    }
}
