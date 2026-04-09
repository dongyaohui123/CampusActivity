package com.campus.activity.dto.v1.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReviewRejectRequest {
    @NotBlank(message = "comment is required")
    @Size(max = 500, message = "comment length must be <= 500")
    private String comment;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
