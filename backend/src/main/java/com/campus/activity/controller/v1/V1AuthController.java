package com.campus.activity.controller.v1;

import com.campus.activity.common.ApiResponse;
import com.campus.activity.dto.v1.auth.LoginRequest;
import com.campus.activity.service.v1.V1AuthService;
import com.campus.activity.view.v1.LoginUserView;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class V1AuthController {
    private final V1AuthService authService;

    public V1AuthController(V1AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginUserView> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("login success", authService.login(request));
    }
}

