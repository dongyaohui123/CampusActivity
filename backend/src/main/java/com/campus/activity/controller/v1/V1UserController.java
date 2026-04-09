package com.campus.activity.controller.v1;

import com.campus.activity.common.ApiResponse;
import com.campus.activity.dto.v1.user.UserProfileUpdateRequest;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.service.v1.V1UserService;
import com.campus.activity.view.v1.RegistrationRecordView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/users")
public class V1UserController {
    private final V1UserService userService;

    public V1UserController(V1UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ApiResponse<User> getUserProfile(
            @PathVariable("userId") @Min(value = 1, message = "userId must be >= 1") Long userId,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success(userService.getUserProfile(userId, operatorUserId, operatorRole));
    }

    @PutMapping("/{userId}/profile")
    public ApiResponse<User> updateUserProfile(
            @PathVariable("userId") @Min(value = 1, message = "userId must be >= 1") Long userId,
            @Valid @RequestBody UserProfileUpdateRequest request,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success("profile updated",
                userService.updateUserProfile(userId, request, operatorUserId, operatorRole));
    }

    @GetMapping("/{userId}/registrations")
    public ApiResponse<List<RegistrationRecordView>> getUserRegistrations(
            @PathVariable("userId") @Min(value = 1, message = "userId must be >= 1") Long userId,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success(userService.getUserRegistrations(userId, operatorUserId, operatorRole));
    }
}
