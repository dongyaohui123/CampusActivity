package com.campus.activity.dto.v1.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * UserProfileUpdateRequest请求参数对象。
 */
public class UserProfileUpdateRequest {
    @Size(max = 100, message = "nickname length must be <= 100")
    private String nickname;

    @Size(max = 255, message = "avatarUrl length must be <= 255")
    private String avatarUrl;

    @Size(max = 20, message = "phone length must be <= 20")
    private String phone;

    @Min(value = 0, message = "gender must be 0/1/2")
    @Max(value = 2, message = "gender must be 0/1/2")
    private Integer gender;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getGender() { return gender; }
    public void setGender(Integer gender) { this.gender = gender; }
}
