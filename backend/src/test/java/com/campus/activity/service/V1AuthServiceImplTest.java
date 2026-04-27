package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.campus.activity.service.impl.v1.V1AuthServiceImpl;
import com.campus.activity.service.v1.WechatAuthGateway;
import com.campus.activity.view.v1.LoginUserView;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class V1AuthServiceImplTest {
    @Mock
    private UserMapper userMapper;

    @Mock
    private WechatAuthGateway wechatAuthGateway;

    @Mock
    private AvatarUrlService avatarUrlService;

    @InjectMocks
    private V1AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        lenient().when(avatarUrlService.normalizeForStorage(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(avatarUrlService.toPublicUrl(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void login_shouldReturnLoginUserViewWhenPasswordCorrect() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPasswordHash("123456");
        user.setNickname("Alice");
        user.setAvatarUrl("/static/avatars/u_1_123.png");
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setPhone("13800000000");
        when(userMapper.selectByUsername("alice")).thenReturn(user);
        when(avatarUrlService.toPublicUrl("/static/avatars/u_1_123.png"))
                .thenReturn("http://127.0.0.1:8080/static/avatars/u_1_123.png");

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("123456");
        LoginUserView result = authService.login(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(result.getAvatarUrl()).isEqualTo("http://127.0.0.1:8080/static/avatars/u_1_123.png");
    }

    @Test
    void login_shouldSupportPhoneWhenUsernameNotFound() {
        User user = new User();
        user.setId(2L);
        user.setUsername("alice");
        user.setPhone("13800000000");
        user.setPasswordHash("123456");
        user.setNickname("Alice");
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        when(userMapper.selectByUsername("13800000000")).thenReturn(null);
        when(userMapper.selectByPhone("13800000000")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("13800000000");
        request.setPassword("123456");
        LoginUserView result = authService.login(request);

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getPhone()).isEqualTo("13800000000");
    }

    @Test
    void login_shouldRejectWhenPasswordWrong() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("right_pwd");
        user.setStatus(UserStatus.ACTIVE);
        when(userMapper.selectByUsername("alice")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("wrong_pwd");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void login_shouldRejectWhenUserDisabled() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("123");
        user.setStatus(UserStatus.DISABLED);
        when(userMapper.selectByUsername("alice")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("123");

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void register_shouldRejectWhenPhoneInvalid() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("123456");
        request.setNickname("Alice");
        request.setPhone("12345");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void register_shouldRejectWhenPhoneDuplicated() {
        User existed = new User();
        existed.setId(100L);
        existed.setPhone("13800000000");
        when(userMapper.selectByPhone("13800000000")).thenReturn(existed);

        RegisterRequest request = new RegisterRequest();
        request.setUsername("alice");
        request.setPassword("123456");
        request.setNickname("Alice");
        request.setPhone("13800000000");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void wechatLogin_shouldReturnExistingUserWhenOpenidExists() {
        User existed = new User();
        existed.setId(10L);
        existed.setUsername("alice");
        existed.setNickname("Alice");
        existed.setRole(UserRole.STUDENT);
        existed.setStatus(UserStatus.ACTIVE);
        existed.setOpenid("wx_open_1");
        when(wechatAuthGateway.exchangeCodeForOpenid("wx-code")).thenReturn("wx_open_1");
        when(userMapper.selectByOpenid("wx_open_1")).thenReturn(existed);

        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("wx-code");

        LoginUserView result = authService.wechatLogin(request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getUsername()).isEqualTo("alice");
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void wechatLogin_shouldAutoCreateWhenOpenidNotExists() {
        when(wechatAuthGateway.exchangeCodeForOpenid("wx-code")).thenReturn("wx_open_2");
        when(userMapper.selectByOpenid("wx_open_2")).thenReturn(null);
        doAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(88L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("wx-code");
        request.setNickname("WechatUser");
        request.setAvatarUrl("https://example.com/a.png");
        request.setGender(1);

        LoginUserView result = authService.wechatLogin(request);

        assertThat(result.getId()).isEqualTo(88L);
        assertThat(result.getRole()).isEqualTo(UserRole.STUDENT);
        verify(userMapper, times(1)).insert(any(User.class));
    }

    @Test
    void wechatLogin_shouldRetryWhenUsernameConflictBeforeInsert() {
        when(wechatAuthGateway.exchangeCodeForOpenid("wx-code")).thenReturn("wx_open_3");
        when(userMapper.selectByOpenid("wx_open_3")).thenReturn(null);
        AtomicInteger count = new AtomicInteger(0);
        doAnswer(invocation -> {
            int current = count.incrementAndGet();
            if (current == 1) {
                throw new DataIntegrityViolationException("Duplicate entry for key 'uk_users_username'");
            }
            User user = invocation.getArgument(0);
            user.setId(99L);
            return 1;
        }).when(userMapper).insert(any(User.class));

        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("wx-code");

        LoginUserView result = authService.wechatLogin(request);

        assertThat(result.getId()).isEqualTo(99L);
        verify(userMapper, times(2)).insert(any(User.class));
    }

    @Test
    void wechatLogin_shouldPropagateBadRequestWhenGatewayFails() {
        when(wechatAuthGateway.exchangeCodeForOpenid("bad-code"))
                .thenThrow(new BusinessException(ErrorCode.BAD_REQUEST, "wechat api error"));

        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("bad-code");

        assertThatThrownBy(() -> authService.wechatLogin(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void wechatLogin_shouldPatchEmptyProfileOnly() {
        User existed = new User();
        existed.setId(66L);
        existed.setUsername("wx_u_xx");
        existed.setOpenid("wx_open_4");
        existed.setNickname("LocalNick");
        existed.setAvatarUrl(null);
        existed.setGender(0);
        existed.setRole(UserRole.STUDENT);
        existed.setStatus(UserStatus.ACTIVE);
        when(wechatAuthGateway.exchangeCodeForOpenid("wx-code")).thenReturn("wx_open_4");
        when(userMapper.selectByOpenid("wx_open_4")).thenReturn(existed);

        WechatLoginRequest request = new WechatLoginRequest();
        request.setCode("wx-code");
        request.setNickname("WechatNick");
        request.setAvatarUrl("https://example.com/new.png");
        request.setGender(2);

        LoginUserView result = authService.wechatLogin(request);

        assertThat(result.getNickname()).isEqualTo("LocalNick");
        verify(userMapper, times(1)).updateById(any(User.class));
        assertThat(existed.getNickname()).isEqualTo("LocalNick");
        assertThat(existed.getAvatarUrl()).isEqualTo("https://example.com/new.png");
        assertThat(existed.getGender()).isEqualTo(2);
    }
}
