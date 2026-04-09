package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * activity_favorites: user favorites for activities.
 */
@Data
@TableName("activity_favorites")
public class ActivityFavorite {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long activityId;

    private Long userId;

    private LocalDateTime createdAt;
}
