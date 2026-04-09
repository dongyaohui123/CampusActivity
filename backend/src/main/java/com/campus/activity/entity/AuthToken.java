package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.activity.enums.TokenType;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * auth_tokens: hashed token records.
 */
@Data
@TableName("auth_tokens")
public class AuthToken {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String tokenHash;

    private TokenType tokenType;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    private Boolean revoked;

    private LocalDateTime revokedAt;

    private String deviceInfo;

    private String ipAddress;

    private LocalDateTime createdAt;
}
