package com.campus.activity.entity.view;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * Activity-category relation projection with category name.
 */
@Data
public class ActivityCategoryLinkView {

    private Long activityId;

    private Long categoryId;

    private String categoryName;

    private LocalDateTime createdAt;
}
