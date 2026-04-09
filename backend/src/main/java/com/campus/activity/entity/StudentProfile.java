package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * student_profiles: one-to-one extension for student users.
 */
@Data
@TableName("student_profiles")
public class StudentProfile {

    /** PK and FK to users.id. */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    private String studentNo;

    private String realName;

    private String college;

    private String major;

    private String className;

    private String grade;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
