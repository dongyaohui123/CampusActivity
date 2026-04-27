package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * activity_category_rel: many-to-many relation between activities and categories.
 *
 * Composite key is (activity_id, category_id). We use custom mapper SQL instead
 * of id-based BaseMapper operations.
 */
@TableName("activity_category_rel")
public class ActivityCategoryRel {

    private Long activityId;

    private Long categoryId;

    private LocalDateTime createdAt;

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
