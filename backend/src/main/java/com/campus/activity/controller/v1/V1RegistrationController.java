package com.campus.activity.controller.v1;

import com.campus.activity.common.ApiResponse;
import com.campus.activity.dto.v1.registration.RegistrationCancelRequest;
import com.campus.activity.dto.v1.registration.RegistrationCreateRequest;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.enums.UserRole;
import com.campus.activity.service.v1.V1RegistrationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/registrations")
public class V1RegistrationController {
    private final V1RegistrationService registrationService;

    public V1RegistrationController(V1RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ApiResponse<ActivityRegistration> register(
            @Valid @RequestBody RegistrationCreateRequest request,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success("registration created",
                registrationService.register(request, operatorUserId, operatorRole));
    }

    @PutMapping("/{registrationId}/cancel")
    public ApiResponse<ActivityRegistration> cancelRegistration(
            @PathVariable("registrationId") @Min(value = 1, message = "registrationId must be >= 1") Long registrationId,
            @Valid @RequestBody(required = false) RegistrationCancelRequest request,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        RegistrationCancelRequest safeRequest = request == null ? new RegistrationCancelRequest() : request;
        return ApiResponse.success("registration cancelled",
                registrationService.cancelRegistration(registrationId, safeRequest, operatorUserId, operatorRole));
    }
}
