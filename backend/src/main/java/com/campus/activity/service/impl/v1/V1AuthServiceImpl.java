package com.campus.activity.service.impl.v1;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.auth.ForgotPasswordRequest;
import com.campus.activity.dto.v1.auth.LoginRequest;
import com.campus.activity.dto.v1.auth.QqLoginRequest;
import com.campus.activity.dto.v1.auth.RegisterRequest;
import com.campus.activity.dto.v1.auth.WechatLoginRequest;
import com.campus.activity.entity.SmsCode;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.SmsCodeMapper;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.v1.AvatarUrlService;
import com.campus.activity.service.v1.QqAuthGateway;
import com.campus.activity.service.v1.V1AuthService;
import com.campus.activity.service.v1.WechatAuthGateway;
import com.campus.activity.view.v1.LoginUserView;
import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Auth service implementation (v1).
 */
@Service
public class V1AuthServiceImpl implements V1AuthService {
    private static final Logger log = LoggerFactory.getLogger(V1AuthServiceImpl.class);
    private static final String PHONE_REGEX = "^1\\d{10}$";
    private static final String WECHAT_USERNAME_PREFIX = "wx_u_";
    private static final String QQ_USERNAME_PREFIX = "qq_u_";
    private static final String QQ_OPENID_PREFIX = "qq:";
    private static final int THIRD_PARTY_CREATE_RETRY_LIMIT = 6;
    private static final int SMS_CODE_LENGTH = 6;
    private static final int SMS_CODE_EXPIRE_MINUTES = 5;
    private static final int SMS_CODE_RESEND_INTERVAL_SECONDS = 60;

    private final UserMapper userMapper;
    private final SmsCodeMapper smsCodeMapper;
    private final WechatAuthGateway wechatAuthGateway;
    private final QqAuthGateway qqAuthGateway;
    private final AvatarUrlService avatarUrlService;

    public V1AuthServiceImpl(
            UserMapper userMapper,
            SmsCodeMapper smsCodeMapper,
            WechatAuthGateway wechatAuthGateway,
            QqAuthGateway qqAuthGateway,
            AvatarUrlService avatarUrlService
    ) {
        this.userMapper = userMapper;
        this.smsCodeMapper = smsCodeMapper;
        this.wechatAuthGateway = wechatAuthGateway;
        this.qqAuthGateway = qqAuthGateway;
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
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名或密码错误");
        }
        if (!StringUtils.hasText(user.getPasswordHash()) || !user.getPasswordHash().equals(password)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "用户名或密码错误");
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
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
        }
        if (!isPhone(phone)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号格式不正确");
        }
        if (userMapper.selectByPhone(phone) != null) {
            throw new BusinessException(ErrorCode.CONFLICT, "手机号已被注册");
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
                throw new BusinessException(ErrorCode.CONFLICT, "手机号已被注册");
            }
            throw new BusinessException(ErrorCode.CONFLICT, "用户名已存在");
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

        return loginWithThirdPartyOpenid(
                openid,
                nickname,
                avatarUrl,
                gender,
                WECHAT_USERNAME_PREFIX,
                "微信用户",
                "wechat login create user failed"
        );
    }

    @Override
    @Transactional
    public LoginUserView qqLogin(QqLoginRequest request) {
        String code = StringUtils.trimWhitespace(request.getCode());
        String rawOpenid = qqAuthGateway.exchangeCodeForOpenid(code);
        String openid = QQ_OPENID_PREFIX + StringUtils.trimWhitespace(rawOpenid);
        String nickname = trimToNull(request.getNickname());
        String avatarUrl = trimToNull(avatarUrlService.normalizeForStorage(request.getAvatarUrl()));
        Integer gender = normalizeGender(request.getGender());

        return loginWithThirdPartyOpenid(
                openid,
                nickname,
                avatarUrl,
                gender,
                QQ_USERNAME_PREFIX,
                "QQ用户",
                "qq login create user failed"
        );
    }

    private LoginUserView loginWithThirdPartyOpenid(
            String openid,
            String nickname,
            String avatarUrl,
            Integer gender,
            String usernamePrefix,
            String defaultNickname,
            String createFailedMessage
    ) {
        User existed = userMapper.selectByOpenid(openid);
        if (existed != null) {
            assertActive(existed);
            patchProfileIfNeeded(existed, nickname, avatarUrl, gender);
            return toLoginView(existed);
        }

        return createThirdPartyUserWithRetry(
                openid,
                nickname,
                avatarUrl,
                gender,
                usernamePrefix,
                defaultNickname,
                createFailedMessage
        );
    }

    private LoginUserView createThirdPartyUserWithRetry(
            String openid,
            String nickname,
            String avatarUrl,
            Integer gender,
            String usernamePrefix,
            String defaultNickname,
            String createFailedMessage
    ) {
        for (int i = 0; i < THIRD_PARTY_CREATE_RETRY_LIMIT; i++) {
            User user = buildThirdPartyUser(openid, nickname, avatarUrl, gender, usernamePrefix, defaultNickname);
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
                throw new BusinessException(ErrorCode.CONFLICT, createFailedMessage);
            }
        }
        throw new BusinessException(ErrorCode.CONFLICT, createFailedMessage);
    }

    private User buildThirdPartyUser(
            String openid,
            String nickname,
            String avatarUrl,
            Integer gender,
            String usernamePrefix,
            String defaultNickname
    ) {
        User user = new User();
        user.setUsername(generateThirdPartyUsername(usernamePrefix));
        user.setPasswordHash(null);
        user.setOpenid(openid);
        user.setNickname(StringUtils.hasText(nickname) ? nickname : defaultNickname);
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

    private String generateThirdPartyUsername(String prefix) {
        long now = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(100000, 999999);
        return prefix + Long.toString(now, 36) + Integer.toString(random, 36);
    }

    @Override
    public void resetPassword(ForgotPasswordRequest request) {
        String username = StringUtils.trimWhitespace(request.getUsername());
        String phone = StringUtils.trimWhitespace(request.getPhone());
        String newPassword = request.getNewPassword();
        String code = StringUtils.trimWhitespace(request.getCode());

        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "用户名不存在");
        }
        if (!phone.equals(user.getPhone())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号与注册信息不一致");
        }
        assertActive(user);

        // 校验验证码
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入验证码");
        }
        verifySmsCode(phone, code);

        if (newPassword.equals(user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "新密码不能与旧密码相同");
        }
        user.setPasswordHash(newPassword);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void sendSmsCode(String phone) {
        String normalizedPhone = StringUtils.trimWhitespace(phone);
        if (!isPhone(normalizedPhone)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "手机号格式不正确");
        }

        // 检查60秒内是否已发送
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(SMS_CODE_RESEND_INTERVAL_SECONDS);
        SmsCode recentCode = smsCodeMapper.selectOne(
                new QueryWrapper<SmsCode>()
                        .eq("phone", normalizedPhone)
                        .eq("purpose", "RESET_PASSWORD")
                        .ge("created_at", cutoff)
                        .last("LIMIT 1")
        );
        if (recentCode != null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码发送过于频繁，请稍后再试");
        }

        // 生成6位随机验证码
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));

        // 保存验证码
        SmsCode smsCode = new SmsCode();
        smsCode.setPhone(normalizedPhone);
        smsCode.setCode(code);
        smsCode.setPurpose("RESET_PASSWORD");
        smsCode.setExpiredAt(LocalDateTime.now().plusMinutes(SMS_CODE_EXPIRE_MINUTES));
        smsCode.setUsed(0);
        smsCodeMapper.insert(smsCode);

        // 模拟模式：输出验证码到日志
        log.info("============================================");
        log.info("[模拟短信] 手机号: {} 验证码: {}", normalizedPhone, code);
        log.info("============================================");
    }

    private void verifySmsCode(String phone, String code) {
        SmsCode smsCode = smsCodeMapper.selectOne(
                new QueryWrapper<SmsCode>()
                        .eq("phone", phone)
                        .eq("purpose", "RESET_PASSWORD")
                        .eq("used", 0)
                        .ge("expired_at", LocalDateTime.now())
                        .orderByDesc("created_at")
                        .last("LIMIT 1")
        );
        if (smsCode == null || !code.equals(smsCode.getCode())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "验证码错误或已过期");
        }
        // 标记验证码已使用
        smsCode.setUsed(1);
        smsCodeMapper.updateById(smsCode);
    }

    private boolean isPhone(String value) {
        return StringUtils.hasText(value) && value.matches(PHONE_REGEX);
    }

    private void assertActive(User user) {
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "账号已被禁用");
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
