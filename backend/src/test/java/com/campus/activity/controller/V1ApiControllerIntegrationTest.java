package com.campus.activity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.User;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.RegistrationStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.service.ActivityService;
import com.campus.activity.service.UserService;
import com.campus.activity.service.v1.V1AdminReviewService;
import com.campus.activity.service.v1.V1AuthService;
import com.campus.activity.service.v1.V1OrganizerActivityService;
import com.campus.activity.service.v1.V1PublicActivityService;
import com.campus.activity.service.v1.V1RegistrationService;
import com.campus.activity.service.v1.V1UserService;
import com.campus.activity.view.v1.AvatarUploadView;
import com.campus.activity.view.v1.LoginUserView;
import java.util.List;
import org.springframework.mock.web.MockMultipartFile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class V1ApiControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private V1UserService v1UserService;

    @MockBean
    private V1PublicActivityService v1PublicActivityService;

    @MockBean
    private V1OrganizerActivityService v1OrganizerActivityService;

    @MockBean
    private V1RegistrationService v1RegistrationService;

    @MockBean
    private V1AdminReviewService v1AdminReviewService;

    @MockBean
    private V1AuthService v1AuthService;

    @Test
    void listPublicActivities_shouldReturnUnifiedSuccessBody() throws Exception {
        ActivityListItemView item = new ActivityListItemView();
        item.setId(1L);
        item.setTitle("Campus Hackday");
        item.setStatus(ActivityStatus.PUBLISHED);
        when(v1PublicActivityService.listPublicActivities(null, null, null)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Campus Hackday"));
    }

    @Test
    void adminPending_shouldReturnForbiddenWhenRoleDenied() throws Exception {
        when(v1AdminReviewService.listPendingReviews(1L, UserRole.STUDENT))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "permission denied"));

        mockMvc.perform(get("/api/v1/admin/reviews/pending")
                        .param("operatorUserId", "1")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void createRegistration_shouldReturnUnifiedSuccessBody() throws Exception {
        ActivityRegistration registration = new ActivityRegistration();
        registration.setId(5L);
        registration.setActivityId(8L);
        registration.setUserId(2L);
        registration.setStatus(RegistrationStatus.REGISTERED);
        when(v1RegistrationService.register(any(), any(), any())).thenReturn(registration);

        String body = """
                {
                  "activityId":8,
                  "remark":"join"
                }
                """;

        mockMvc.perform(post("/api/v1/registrations")
                        .param("operatorUserId", "2")
                        .param("operatorRole", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("registration created"))
                .andExpect(jsonPath("$.data.id").value(5));
    }

    @Test
    void changePassword_shouldReturnUnifiedSuccessBody() throws Exception {
        String body = """
                {
                  "oldPassword":"123456",
                  "newPassword":"654321"
                }
                """;

        mockMvc.perform(put("/api/v1/users/2/password")
                        .param("operatorUserId", "2")
                        .param("operatorRole", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("password changed"));
    }

    @Test
    void changePassword_shouldValidateRequiredFields() throws Exception {
        String body = "{}";

        mockMvc.perform(put("/api/v1/users/2/password")
                        .param("operatorUserId", "2")
                        .param("operatorRole", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void rejectReview_shouldValidateCommentRequired() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/api/v1/admin/reviews/8/reject")
                        .param("operatorUserId", "1")
                        .param("operatorRole", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void wechatLogin_shouldValidateCodeRequired() throws Exception {
        String body = "{}";
        mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void wechatLogin_shouldReturnUnifiedSuccessBody() throws Exception {
        LoginUserView view = new LoginUserView();
        view.setId(12L);
        view.setUsername("wx_u_abc");
        view.setNickname("WechatUser");
        view.setRole(UserRole.STUDENT);
        when(v1AuthService.wechatLogin(any())).thenReturn(view);

        String body = """
                {
                  "code":"wx-login-code",
                  "nickname":"WechatUser"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/wechat-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("wechat login success"))
                .andExpect(jsonPath("$.data.id").value(12))
                .andExpect(jsonPath("$.data.username").value("wx_u_abc"));
    }

    @Test
    void uploadAvatar_shouldReturnUnifiedSuccessBody() throws Exception {
        AvatarUploadView view = new AvatarUploadView();
        view.setAvatarUrl("http://127.0.0.1:8080/static/avatars/u_12_123456_654321.png");
        when(v1UserService.uploadAvatar(any(), any(), any(), any())).thenReturn(view);

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", "avatar".getBytes());
        mockMvc.perform(multipart("/api/v1/users/12/avatar")
                        .file(file)
                        .param("operatorUserId", "12")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("avatar uploaded"))
                .andExpect(jsonPath("$.data.avatarUrl").value("http://127.0.0.1:8080/static/avatars/u_12_123456_654321.png"));
    }

    @Test
    void uploadAvatar_shouldReturnBadRequestWhenFileMissing() throws Exception {
        when(v1UserService.uploadAvatar(any(), isNull(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.BAD_REQUEST, "avatar file is required"));

        mockMvc.perform(multipart("/api/v1/users/12/avatar")
                        .param("operatorUserId", "12")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void uploadAvatar_shouldReturnBadRequestWhenFileTypeInvalid() throws Exception {
        when(v1UserService.uploadAvatar(any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.BAD_REQUEST, "avatar image type must be jpeg/png/webp"));

        MockMultipartFile file = new MockMultipartFile("file", "avatar.gif", "image/gif", "gif".getBytes());
        mockMvc.perform(multipart("/api/v1/users/12/avatar")
                        .file(file)
                        .param("operatorUserId", "12")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void uploadAvatar_shouldReturnBadRequestWhenFileTooLarge() throws Exception {
        when(v1UserService.uploadAvatar(any(), any(), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.BAD_REQUEST, "avatar image size must be <= 1024KB"));

        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[1025 * 1024]);
        mockMvc.perform(multipart("/api/v1/users/12/avatar")
                        .file(file)
                        .param("operatorUserId", "12")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    void updateProfile_shouldReturnUnifiedSuccessBody() throws Exception {
        User user = new User();
        user.setId(12L);
        user.setUsername("wx_u_12");
        user.setNickname("NewNick");
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);
        when(v1UserService.updateUserProfile(any(), any(), any(), any())).thenReturn(user);

        String body = """
                {
                  "nickname":"NewNick"
                }
                """;
        mockMvc.perform(put("/api/v1/users/12/profile")
                        .param("operatorUserId", "12")
                        .param("operatorRole", "STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("profile updated"))
                .andExpect(jsonPath("$.data.nickname").value("NewNick"));
    }
}
