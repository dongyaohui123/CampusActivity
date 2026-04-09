package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * activity_category_rel: many-to-many relation between activities and categories.
 *
 * Composite key is (activity_id, category_id). We use custom mapper SQL instead
 * of id-based BaseMapper operations.
 */
@Data
@TableName("activity_category_rel")
public class ActivityCategoryRel {

    private Long activityId;

    private Long categoryId;

    private LocalDateTime createdAt;
}
