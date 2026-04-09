package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.activity.enums.CertificationStatus;
import com.campus.activity.enums.OrganizerType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * organizer_profiles: one-to-one extension for organizer users.
 */
@Data
@TableName("organizer_profiles")
public class OrganizerProfile {

    /** PK and FK to users.id. */
    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    private String organizerName;

    private OrganizerType organizerType;

    private String contactName;

    private String contactPhone;

    private String intro;

    private CertificationStatus certificationStatus;

    private LocalDateTime certifiedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
