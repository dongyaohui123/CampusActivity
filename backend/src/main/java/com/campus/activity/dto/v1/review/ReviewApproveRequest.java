package com.campus.activity.dto.v1.review;

import jakarta.validation.constraints.Size;

/**
 * ReviewApproveRequest请求参数对象。
 */
public class ReviewApproveRequest {
    @Size(max = 500, message = "comment length must be <= 500")
    private String comment;
    private Boolean featured;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }
}
