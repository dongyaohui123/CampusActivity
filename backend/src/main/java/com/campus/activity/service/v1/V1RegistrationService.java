package com.campus.activity.service.v1;

import com.campus.activity.dto.v1.registration.RegistrationCancelRequest;
import com.campus.activity.dto.v1.registration.RegistrationCreateRequest;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.enums.UserRole;

public interface V1RegistrationService {
    ActivityRegistration register(RegistrationCreateRequest request, Long operatorUserId, UserRole operatorRole);

    ActivityRegistration cancelRegistration(Long registrationId, RegistrationCancelRequest request, Long operatorUserId, UserRole operatorRole);
}
