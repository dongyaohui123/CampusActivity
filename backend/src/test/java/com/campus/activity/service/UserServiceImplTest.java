package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.user.CreateUserRequest;
import com.campus.activity.dto.user.UpdateUserRequest;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {
    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void createUser_shouldSetDefaultsAndSanitizePassword() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("alice");
        request.setPasswordHash("hash-123");

        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User arg = invocation.getArgument(0);
            arg.setId(1L);
            return 1;
        });

        User created = userService.createUser(request);

        assertThat(created.getId()).isEqualTo(1L);
        assertThat(created.getRole()).isEqualTo(UserRole.STUDENT);
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.getForcePasswordChange()).isFalse();
        assertThat(created.getPasswordHash()).isNull();
    }

    @Test
    void createUser_shouldTranslateDuplicateUsername() {
        CreateUserRequest request = new CreateUserRequest();
        request.setUsername("alice");

        when(userMapper.insert(any(User.class))).thenThrow(new DuplicateKeyException("uk_users_username"));

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void disableUser_shouldSetDisabledStatus() {
        User existing = new User();
        existing.setId(2L);
        existing.setStatus(UserStatus.ACTIVE);
        when(userMapper.selectById(2L)).thenReturn(existing);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        userService.disableUser(2L);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(UserStatus.DISABLED);
    }

    @Test
    void updateUser_shouldThrowNotFoundWhenMissing() {
        when(userMapper.selectById(99L)).thenReturn(null);
        UpdateUserRequest request = new UpdateUserRequest();
        request.setNickname("neo");

        assertThatThrownBy(() -> userService.updateUser(99L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }
}
