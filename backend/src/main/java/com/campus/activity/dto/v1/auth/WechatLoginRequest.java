package com.campus.activity.dto.v1.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * WeChat mini-program login request.
 */
public class WechatLoginRequest {
    @NotBlank(message = "code is required")
    @Size(max = 128, message = "code length must be <= 128")
    private String code;

    @Size(max = 100, message = "nickname length must be <= 100")
    private String nickname;

    @Size(max = 255, message = "avatarUrl length must be <= 255")
    private String avatarUrl;

    @Min(value = 0, message = "gender must be between 0 and 2")
    @Max(value = 2, message = "gender must be between 0 and 2")
    private Integer gender;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }
}
