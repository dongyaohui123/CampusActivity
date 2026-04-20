package com.campus.activity.view.v1;

import com.campus.activity.enums.RegistrationStatus;
import java.time.LocalDateTime;

/**
 * ActivityRegistrationUserView页面视图对象。
 */
public class ActivityRegistrationUserView {
    private Long registrationId;
    private Long userId;
    private String nickname;
    private String phone;
    private RegistrationStatus status;
    private String remark;
    private LocalDateTime registeredAt;
    private LocalDateTime cancelledAt;

    public Long getRegistrationId() { return registrationId; }
    public void setRegistrationId(Long registrationId) { this.registrationId = registrationId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(LocalDateTime registeredAt) { this.registeredAt = registeredAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
}
