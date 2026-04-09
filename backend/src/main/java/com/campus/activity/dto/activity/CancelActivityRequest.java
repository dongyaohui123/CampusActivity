package com.campus.activity.dto.activity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload for logical cancel operation.
 */
public class CancelActivityRequest {
    @NotBlank(message = "cancelReason is required")
    @Size(max = 255, message = "cancelReason length must be <= 255")
    private String cancelReason;

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }
}
