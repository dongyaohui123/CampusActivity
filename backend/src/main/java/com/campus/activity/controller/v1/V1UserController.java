package com.campus.activity.controller.v1;

import com.campus.activity.common.ApiResponse;
import com.campus.activity.dto.v1.user.UserPasswordChangeRequest;
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

/**
 * 用户中心控制器（v1）。
 * 提供用户资料查询/更新和报名记录查询。
 */
@Validated
@RestController
@RequestMapping("/api/v1/users")
public class V1UserController {
    private final V1UserService userService;

    /**
     * 构造函数。
     *
     * @param userService 用户服务
     */
    public V1UserController(V1UserService userService) {
        this.userService = userService;
    }

    /**
     * 查询用户资料。
     *
     * @param userId 目标用户 ID
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 用户资料
     */
    @GetMapping("/{userId}")
    public ApiResponse<User> getUserProfile(
            @PathVariable("userId") @Min(value = 1, message = "userId must be >= 1") Long userId,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success(userService.getUserProfile(userId, operatorUserId, operatorRole));
    }

    /**
     * 更新用户资料。
     *
     * @param userId 目标用户 ID
     * @param request 更新参数
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 更新后的用户资料
     */
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

    /**
     * 修改用户密码。
     *
     * @param userId 目标用户 ID
     * @param request 修改密码参数
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 统一成功响应
     */
    @PutMapping("/{userId}/password")
    public ApiResponse<Void> changeUserPassword(
            @PathVariable("userId") @Min(value = 1, message = "userId must be >= 1") Long userId,
            @Valid @RequestBody UserPasswordChangeRequest request,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        userService.changeUserPassword(userId, request, operatorUserId, operatorRole);
        return ApiResponse.success("password changed", null);
    }

    /**
     * 查询用户报名记录。
     *
     * @param userId 目标用户 ID
     * @param operatorUserId 操作人 ID
     * @param operatorRole 操作人角色
     * @return 报名记录列表
     */
    @GetMapping("/{userId}/registrations")
    public ApiResponse<List<RegistrationRecordView>> getUserRegistrations(
            @PathVariable("userId") @Min(value = 1, message = "userId must be >= 1") Long userId,
            @RequestParam("operatorUserId") @Min(value = 1, message = "operatorUserId must be >= 1") Long operatorUserId,
            @RequestParam("operatorRole") UserRole operatorRole
    ) {
        return ApiResponse.success(userService.getUserRegistrations(userId, operatorUserId, operatorRole));
    }
}
