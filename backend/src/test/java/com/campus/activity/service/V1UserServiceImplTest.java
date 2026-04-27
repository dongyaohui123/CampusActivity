package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.user.UserPasswordChangeRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.impl.v1.V1UserServiceImpl;
import com.campus.activity.service.v1.OperatorPermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class V1UserServiceImplTest {
    @Mock
    private UserMapper userMapper;

    @Mock
    private ActivityRegistrationMapper registrationMapper;

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private OperatorPermissionService permissionService;

    @InjectMocks
    private V1UserServiceImpl userService;

    @Test
    void changeUserPassword_shouldUpdatePasswordWhenOldPasswordMatches() {
        User operator = buildUser(1L, "old-password");
        User target = buildUser(1L, "old-password");
        target.setForcePasswordChange(Boolean.TRUE);

        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        when(userMapper.selectById(1L)).thenReturn(target);

        UserPasswordChangeRequest request = new UserPasswordChangeRequest();
        request.setOldPassword("old-password");
        request.setNewPassword("new-password");

        userService.changeUserPassword(1L, request, 1L, UserRole.STUDENT);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("new-password");
        assertThat(captor.getValue().getForcePasswordChange()).isFalse();
    }

    @Test
    void changeUserPassword_shouldRejectWhenOldPasswordWrong() {
        User operator = buildUser(1L, "old-password");
        User target = buildUser(1L, "stored-password");

        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        when(userMapper.selectById(1L)).thenReturn(target);

        UserPasswordChangeRequest request = new UserPasswordChangeRequest();
        request.setOldPassword("wrong-password");
        request.setNewPassword("new-password");

        assertThatThrownBy(() -> userService.changeUserPassword(1L, request, 1L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void changeUserPassword_shouldRejectWhenNewPasswordSameAsOldPassword() {
        User operator = buildUser(1L, "same-password");
        User target = buildUser(1L, "same-password");

        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        when(userMapper.selectById(1L)).thenReturn(target);

        UserPasswordChangeRequest request = new UserPasswordChangeRequest();
        request.setOldPassword("same-password");
        request.setNewPassword("same-password");

        assertThatThrownBy(() -> userService.changeUserPassword(1L, request, 1L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void changeUserPassword_shouldRejectWhenUserMissing() {
        User operator = buildUser(1L, "old-password");

        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        when(userMapper.selectById(9L)).thenReturn(null);

        UserPasswordChangeRequest request = new UserPasswordChangeRequest();
        request.setOldPassword("old-password");
        request.setNewPassword("new-password");

        assertThatThrownBy(() -> userService.changeUserPassword(9L, request, 1L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void changeUserPassword_shouldRejectWhenOperatorIsNotSelf() {
        User operator = buildUser(1L, "old-password");

        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        doThrow(new BusinessException(ErrorCode.FORBIDDEN, "only self can access this resource"))
                .when(permissionService).requireSelf(any(User.class), any(Long.class));

        UserPasswordChangeRequest request = new UserPasswordChangeRequest();
        request.setOldPassword("old-password");
        request.setNewPassword("new-password");

        assertThatThrownBy(() -> userService.changeUserPassword(2L, request, 1L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    private User buildUser(Long id, String passwordHash) {
        User user = new User();
        user.setId(id);
        user.setPasswordHash(passwordHash);
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }
}
