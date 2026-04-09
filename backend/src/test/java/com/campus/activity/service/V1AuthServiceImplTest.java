package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.auth.LoginRequest;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.impl.v1.V1AuthServiceImpl;
import com.campus.activity.view.v1.LoginUserView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class V1AuthServiceImplTest {
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private V1AuthServiceImpl authService;

    @Test
    void login_shouldReturnLoginUserViewWhenPasswordCorrect() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setPasswordHash("123456");
        user.setNickname("Alice");
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        user.setPhone("13800000000");
        when(userMapper.selectByUsername("alice")).thenReturn(user);

        LoginRequest request = new LoginRequest();
        request.setUsername("alice");
        request.setPassword("123456");
        LoginUserView result = authService.login(request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
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
}

