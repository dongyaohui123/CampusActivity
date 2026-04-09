package com.campus.activity.controller;

import com.campus.activity.common.ApiResponse;
import com.campus.activity.common.PageResponse;
import com.campus.activity.dto.user.CreateUserRequest;
import com.campus.activity.dto.user.UpdateUserRequest;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ApiResponse<User> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success("user created", userService.createUser(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(
            @PathVariable("id") @Min(value = 1, message = "id must be >= 1") Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return ApiResponse.success("user updated", userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> disableUser(
            @PathVariable("id") @Min(value = 1, message = "id must be >= 1") Long id
    ) {
        userService.disableUser(id);
        return ApiResponse.success("user disabled", Boolean.TRUE);
    }

    @GetMapping("/{id}")
    public ApiResponse<User> getUserById(
            @PathVariable("id") @Min(value = 1, message = "id must be >= 1") Long id
    ) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<User>> listUsers(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "status", required = false) UserStatus status,
            @RequestParam(value = "role", required = false) UserRole role,
            @RequestParam(value = "page", defaultValue = "1") @Min(value = 1, message = "page must be >= 1") long page,
            @RequestParam(value = "size", defaultValue = "10") @Min(value = 1, message = "size must be >= 1")
            @Max(value = 100, message = "size must be <= 100") long size
    ) {
        return ApiResponse.success(userService.listUsers(username, status, role, page, size));
    }
}
