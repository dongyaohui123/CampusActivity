package com.campus.activity.service.v1;

import com.campus.activity.dto.v1.user.UserProfileUpdateRequest;
import com.campus.activity.entity.User;
import com.campus.activity.view.v1.RegistrationRecordView;
import java.util.List;

public interface V1UserService {
    User getUserProfile(Long userId, Long operatorUserId, com.campus.activity.enums.UserRole operatorRole);

    User updateUserProfile(Long userId, UserProfileUpdateRequest request, Long operatorUserId, com.campus.activity.enums.UserRole operatorRole);

    List<RegistrationRecordView> getUserRegistrations(Long userId, Long operatorUserId, com.campus.activity.enums.UserRole operatorRole);
}
