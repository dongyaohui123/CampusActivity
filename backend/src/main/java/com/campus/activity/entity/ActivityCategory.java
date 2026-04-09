package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.activity.enums.BasicStatus;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * activity_categories: activity category tree.
 */
@Data
@TableName("activity_categories")
public class ActivityCategory {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long parentId;

    private String name;

    private Integer sortNo;

    private BasicStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
