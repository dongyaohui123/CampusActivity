package com.campus.activity.service.v1;

import com.campus.activity.dto.v1.auth.LoginRequest;
import com.campus.activity.view.v1.LoginUserView;

public interface V1AuthService {
    LoginUserView login(LoginRequest request);
}

