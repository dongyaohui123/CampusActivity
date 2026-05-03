package com.campus.activity.dto.v1.activity;

import jakarta.validation.constraints.NotNull;

/**
 * Activity comment create request.
 */
public class CreateActivityCommentRequest {
    @NotNull(message = "content is required")
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
