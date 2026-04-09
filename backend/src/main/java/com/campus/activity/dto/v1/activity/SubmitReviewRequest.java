package com.campus.activity.dto.v1.activity;

import jakarta.validation.constraints.Size;

public class SubmitReviewRequest {
    @Size(max = 500, message = "comment length must be <= 500")
    private String comment;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
