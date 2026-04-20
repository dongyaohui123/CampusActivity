package com.campus.activity.service.impl.v1;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.auth.LoginRequest;
import com.campus.activity.dto.v1.auth.RegisterRequest;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.v1.V1AuthService;
import com.campus.activity.view.v1.LoginUserView;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * V1AuthServiceImpl服务实现。
 */
@Service
public class V1AuthServiceImpl implements V1AuthService {
    private static final String PHONE_REGEX = "^1\\d{10}$";
    private final UserMapper userMapper;

    /**
     * 构造函数。
     *
     * @param userMapper 用户数据访问
     */
    public V1AuthServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求
     * @return 登录用户信息
     */
    @Override
    public LoginUserView login(LoginRequest request) {
        String account = StringUtils.trimWhitespace(request.getUsername());
        String password = request.getPassword();
        User user = userMapper.selectByUsername(account);
        if (user == null && isPhone(account)) {
            user = userMapper.selectByPhone(account);
        }
        if (user == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "account or password is incorrect");
        }
        if (!StringUtils.hasText(user.getPasswordHash()) || !user.getPasswordHash().equals(password)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "account or password is incorrect");
        }
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "account is not active");
        }

        return toLoginView(user);
    }

    /**
     * 用户注册。
     *
     * @param request 注册请求
     * @return 注册后登录用户信息
     */
    @Override
    @Transactional
    public LoginUserView register(RegisterRequest request) {
        String username = StringUtils.trimWhitespace(request.getUsername());
        String password = request.getPassword();
        String nickname = StringUtils.trimWhitespace(request.getNickname());
        String phone = StringUtils.trimWhitespace(request.getPhone());

        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "username already exists");
        }
        if (!isPhone(phone)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "phone format is invalid");
        }
        if (userMapper.selectByPhone(phone) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "phone already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(password);
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setForcePasswordChange(Boolean.FALSE);

        try {
            userMapper.insert(user);
        } catch (DataIntegrityViolationException ex) {
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            if (message.contains("uk_users_phone")) {
                throw new BusinessException(ErrorCode.CONFLICT, "phone already exists");
            }
            throw new BusinessException(ErrorCode.CONFLICT, "username already exists");
        }
        return toLoginView(user);
    }

    private boolean isPhone(String value) {
        return StringUtils.hasText(value) && value.matches(PHONE_REGEX);
    }

    /**
     * 将用户实体转换为登录视图。
     */
    private LoginUserView toLoginView(User user) {
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
