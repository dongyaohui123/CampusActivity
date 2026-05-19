package com.campus.activity.dto.v1.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * RegisterRequest请求参数对象。
 */
public class RegisterRequest {
    @NotBlank(message = "username is required")
    @Size(max = 64, message = "username length must be <= 64")
    private String username;

    @NotBlank(message = "password is required")
    @Size(max = 255, message = "password length must be <= 255")
    private String password;

    @NotBlank(message = "nickname is required")
    @Size(max = 100, message = "nickname length must be <= 100")
    private String nickname;

    @NotBlank(message = "phone is required")
    @Size(max = 20, message = "phone length must be <= 20")
    @Pattern(regexp = "^1\\d{10}$", message = "phone format is invalid")
    private String phone;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}

