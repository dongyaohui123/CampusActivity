package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * organizer_follows: user follows for organizer accounts.
 */
@Data
@TableName("organizer_follows")
public class OrganizerFollow {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long organizerId;

    private Long userId;

    private LocalDateTime createdAt;
}
