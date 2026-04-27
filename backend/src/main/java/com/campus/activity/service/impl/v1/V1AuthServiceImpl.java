package com.campus.activity.service.impl.v1;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.auth.LoginRequest;
import com.campus.activity.dto.v1.auth.RegisterRequest;
import com.campus.activity.dto.v1.auth.WechatLoginRequest;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.v1.AvatarUrlService;
import com.campus.activity.service.v1.V1AuthService;
import com.campus.activity.service.v1.WechatAuthGateway;
import com.campus.activity.view.v1.LoginUserView;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Auth service implementation (v1).
 */
@Service
public class V1AuthServiceImpl implements V1AuthService {
    private static final String PHONE_REGEX = "^1\\d{10}$";
    private static final String WECHAT_USERNAME_PREFIX = "wx_u_";
    private static final int WECHAT_CREATE_RETRY_LIMIT = 6;
    private final UserMapper userMapper;
    private final WechatAuthGateway wechatAuthGateway;
    private final AvatarUrlService avatarUrlService;

    public V1AuthServiceImpl(
            UserMapper userMapper,
            WechatAuthGateway wechatAuthGateway,
            AvatarUrlService avatarUrlService
    ) {
        this.userMapper = userMapper;
        this.wechatAuthGateway = wechatAuthGateway;
        this.avatarUrlService = avatarUrlService;
    }

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
        assertActive(user);
        return toLoginView(user);
    }

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

    @Override
    @Transactional
    public LoginUserView wechatLogin(WechatLoginRequest request) {
        String code = StringUtils.trimWhitespace(request.getCode());
        String openid = wechatAuthGateway.exchangeCodeForOpenid(code);
        String nickname = trimToNull(request.getNickname());
        String avatarUrl = trimToNull(avatarUrlService.normalizeForStorage(request.getAvatarUrl()));
        Integer gender = normalizeGender(request.getGender());

        User existed = userMapper.selectByOpenid(openid);
        if (existed != null) {
            assertActive(existed);
            patchProfileIfNeeded(existed, nickname, avatarUrl, gender);
            return toLoginView(existed);
        }

        return createWechatUserWithRetry(openid, nickname, avatarUrl, gender);
    }

    private LoginUserView createWechatUserWithRetry(String openid, String nickname, String avatarUrl, Integer gender) {
        for (int i = 0; i < WECHAT_CREATE_RETRY_LIMIT; i++) {
            User user = buildWechatUser(openid, nickname, avatarUrl, gender);
            try {
                userMapper.insert(user);
                return toLoginView(user);
            } catch (DataIntegrityViolationException ex) {
                String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
                if (message.contains("uk_users_username")) {
                    continue;
                }
                if (message.contains("uk_users_openid")) {
                    User racedUser = userMapper.selectByOpenid(openid);
                    if (racedUser != null) {
                        assertActive(racedUser);
                        patchProfileIfNeeded(racedUser, nickname, avatarUrl, gender);
                        return toLoginView(racedUser);
                    }
                }
                throw new BusinessException(ErrorCode.CONFLICT, "wechat login create user failed");
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT, "wechat login create user failed");
    }

    private User buildWechatUser(String openid, String nickname, String avatarUrl, Integer gender) {
        User user = new User();
        user.setUsername(generateWechatUsername());
        user.setPasswordHash(null);
        user.setOpenid(openid);
        user.setNickname(StringUtils.hasText(nickname) ? nickname : "微信用户");
        user.setAvatarUrl(avatarUrl);
        user.setGender(gender != null ? gender : 0);
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setForcePasswordChange(Boolean.FALSE);
        return user;
    }

    private void patchProfileIfNeeded(User user, String nickname, String avatarUrl, Integer gender) {
        boolean changed = false;
        if (!StringUtils.hasText(user.getNickname()) && StringUtils.hasText(nickname)) {
            user.setNickname(nickname);
            changed = true;
        }
        if (!StringUtils.hasText(user.getAvatarUrl()) && StringUtils.hasText(avatarUrl)) {
            user.setAvatarUrl(avatarUrl);
            changed = true;
        }
        if ((user.getGender() == null || user.getGender() == 0) && gender != null && gender > 0) {
            user.setGender(gender);
            changed = true;
        }
        if (changed) {
            userMapper.updateById(user);
        }
    }

    private Integer normalizeGender(Integer gender) {
        if (gender == null || gender < 0 || gender > 2) {
            return null;
        }
        return gender;
    }

    private String trimToNull(String value) {
        String trimmed = StringUtils.trimWhitespace(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private String generateWechatUsername() {
        long now = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        return WECHAT_USERNAME_PREFIX + Long.toString(now, 36) + Integer.toString(random, 36);
    }

    private boolean isPhone(String value) {
        return StringUtils.hasText(value) && value.matches(PHONE_REGEX);
    }

    private void assertActive(User user) {
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "account is not active");
        }
    }

    private LoginUserView toLoginView(User user) {
        LoginUserView view = new LoginUserView();
        view.setId(user.getId());
        view.setUsername(user.getUsername());
        view.setNickname(user.getNickname());
        view.setRole(user.getRole());
        view.setStatus(user.getStatus());
        view.setAvatarUrl(avatarUrlService.toPublicUrl(user.getAvatarUrl()));
        view.setPhone(user.getPhone());
        return view;
    }
}
