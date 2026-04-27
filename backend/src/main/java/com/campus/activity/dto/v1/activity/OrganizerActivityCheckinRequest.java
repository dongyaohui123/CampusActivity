package com.campus.activity.dto.v1.activity;

import jakarta.validation.constraints.NotBlank;

/**
 * 组织者扫码签到请求。
 */
public class OrganizerActivityCheckinRequest {

    @NotBlank(message = "ticketCode is required")
    private String ticketCode;

    public String getTicketCode() {
        return ticketCode;
    }

    public void setTicketCode(String ticketCode) {
        this.ticketCode = ticketCode;
    }
}
