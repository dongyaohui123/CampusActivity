package com.campus.activity.service.impl.v1;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.auth.LoginRequest;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.v1.V1AuthService;
import com.campus.activity.view.v1.LoginUserView;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class V1AuthServiceImpl implements V1AuthService {
    private final UserMapper userMapper;

    public V1AuthServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public LoginUserView login(LoginRequest request) {
        String username = StringUtils.trimWhitespace(request.getUsername());
        String password = request.getPassword();
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "username or password is incorrect");
        }
        if (!StringUtils.hasText(user.getPasswordHash()) || !user.getPasswordHash().equals(password)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "username or password is incorrect");
        }
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "account is not active");
        }

        LoginUserView view = new LoginUserView();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setNickname(user.getNickname());
        view.setRole(user.getRole());
        view.setStatus(user.getStatus());
        view.setAvatarUrl(user.getAvatarUrl());
        view.setPhone(user.getPhone());
        return view;
    }
}

