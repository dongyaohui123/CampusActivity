package com.campus.activity.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.campus.activity.enums.LoginResult;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * login_logs: login attempts and outcomes.
 */
@Data
@TableName("login_logs")
public class LoginLog {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String usernameAttempt;

    private LoginResult loginResult;

    private String failureReason;

    private String ipAddress;

    private String userAgent;

    private LocalDateTime loggedAt;
}
