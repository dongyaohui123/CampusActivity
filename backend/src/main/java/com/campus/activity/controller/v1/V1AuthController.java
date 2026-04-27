package com.campus.activity.controller.v1;

import com.campus.activity.common.ApiResponse;
import com.campus.activity.dto.v1.auth.LoginRequest;
import com.campus.activity.dto.v1.auth.RegisterRequest;
import com.campus.activity.service.v1.V1AuthService;
import com.campus.activity.view.v1.LoginUserView;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器（v1）。
 * 提供登录与注册能力，并返回登录态所需用户信息。
 */
@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class V1AuthController {
    private final V1AuthService authService;

    /**
     * 构造函数。
     *
     * @param authService 认证服务
     */
    public V1AuthController(V1AuthService authService) {
        this.authService = authService;
    }

    /**
     * 用户登录。
     *
     * @param request 登录参数
     * @return 登录用户信息
     */
    @PostMapping("/login")
    public ApiResponse<LoginUserView> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("login success", authService.login(request));
    }

    /**
     * 用户注册。
     *
     * @param request 注册参数
     * @return 注册并自动登录后的用户信息
     */
    @PostMapping("/register")
    public ApiResponse<LoginUserView> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("register success", authService.register(request));
    }
}
