package com.campus.activity.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.campus.activity.service.v1.V1ActivityFavoriteService;
import com.campus.activity.service.v1.V1AuthService;
import com.campus.activity.service.v1.V1OrganizerActivityService;
import com.campus.activity.service.v1.V1PublicActivityService;
import com.campus.activity.service.v1.V1RegistrationService;
import com.campus.activity.service.v1.V1UserService;
import com.campus.activity.view.v1.ActivityFavoriteStateView;
import com.campus.activity.view.v1.AvatarUploadView;
import com.campus.activity.view.v1.CheckinResultView;
import com.campus.activity.view.v1.FavoriteActivityView;
import com.campus.activity.view.v1.LoginUserView;
import com.campus.activity.view.v1.OrganizerActivityOptionsView;
import com.campus.activity.view.v1.OrganizerActivityTypeOptionView;
import com.campus.activity.view.v1.TicketDetailView;
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

    @MockBean
    private V1ActivityFavoriteService v1ActivityFavoriteService;

    @Test
    void listPublicActivities_shouldReturnUnifiedSuccessBody() throws Exception {
        ActivityListItemView item = new ActivityListItemView();
        item.setId(1L);
        item.setTitle("Campus Hackday");
        item.setStatus(ActivityStatus.PUBLISHED);
        item.setLocationCampus("SOUTH");
        when(v1PublicActivityService.listPublicActivities(null, null, null)).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Campus Hackday"))
                .andExpect(jsonPath("$.data[0].locationCampus").value("SOUTH"));
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
    void getTicketDetail_shouldReturnUnifiedSuccessBody() throws Exception {
        TicketDetailView view = new TicketDetailView();
        view.setRegistrationId(5L);
        view.setActivityId(8L);
        view.setTicketCode("TICKET-001");
        when(v1RegistrationService.getTicketDetail(5L, 2L, UserRole.STUDENT)).thenReturn(view);

        mockMvc.perform(get("/api/v1/registrations/5/ticket")
                        .param("operatorUserId", "2")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ticket detail"))
                .andExpect(jsonPath("$.data.registrationId").value(5))
                .andExpect(jsonPath("$.data.ticketCode").value("TICKET-001"));
    }

    @Test
    void getTicketQrCode_shouldReturnPngBody() throws Exception {
        when(v1RegistrationService.renderTicketQrCode(5L, 2L, UserRole.STUDENT))
                .thenReturn("PNG".getBytes());

        mockMvc.perform(get("/api/v1/registrations/5/ticket/qrcode")
                        .param("operatorUserId", "2")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    assertThat(result.getResponse().getContentType()).isEqualTo(MediaType.IMAGE_PNG_VALUE);
                    assertThat(result.getResponse().getContentAsByteArray()).isEqualTo("PNG".getBytes());
                });
    }

    @Test
    void getTicketQrCode_shouldReturnForbiddenWhenServiceRejects() throws Exception {
        when(v1RegistrationService.renderTicketQrCode(5L, 9L, UserRole.STUDENT))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "only self or admin can access this resource"));

        mockMvc.perform(get("/api/v1/registrations/5/ticket/qrcode")
                        .param("operatorUserId", "9")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
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
    void organizerCheckIn_shouldValidateTicketCodeRequired() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/api/v1/organizer/activities/8/check-in")
                        .param("operatorUserId", "1")
                        .param("operatorRole", "ORGANIZER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void organizerCheckIn_shouldReturnUnifiedSuccessBody() throws Exception {
        CheckinResultView view = new CheckinResultView();
        view.setRegistrationId(5L);
        view.setActivityId(8L);
        view.setUserId(2L);
        when(v1OrganizerActivityService.checkInByTicketCode(any(), any(), any(), any())).thenReturn(view);

        String body = """
                {
                  "ticketCode":"TICKET-001"
                }
                """;

        mockMvc.perform(post("/api/v1/organizer/activities/8/check-in")
                        .param("operatorUserId", "1")
                        .param("operatorRole", "ORGANIZER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("check-in success"))
                .andExpect(jsonPath("$.data.registrationId").value(5));
    }

    @Test
    void organizerActivityOptions_shouldReturnUnifiedSuccessBody() throws Exception {
        OrganizerActivityTypeOptionView typeOption = new OrganizerActivityTypeOptionView();
        typeOption.setId(7L);
        typeOption.setName("算法竞赛");

        OrganizerActivityOptionsView optionsView = new OrganizerActivityOptionsView();
        optionsView.setCampusTypes(List.of("SOUTH", "NORTH", "ONLINE"));
        optionsView.setActivityTypes(List.of(typeOption));
        when(v1OrganizerActivityService.getActivityOptions(1L, UserRole.ORGANIZER)).thenReturn(optionsView);

        mockMvc.perform(get("/api/v1/organizer/activities/options")
                        .param("operatorUserId", "1")
                        .param("operatorRole", "ORGANIZER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.campusTypes[0]").value("SOUTH"))
                .andExpect(jsonPath("$.data.campusTypes[1]").value("NORTH"))
                .andExpect(jsonPath("$.data.campusTypes[2]").value("ONLINE"))
                .andExpect(jsonPath("$.data.activityTypes[0].id").value(7))
                .andExpect(jsonPath("$.data.activityTypes[0].name").value("算法竞赛"));
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

    @Test
    void getFavoriteState_shouldReturnUnifiedSuccessBody() throws Exception {
        ActivityFavoriteStateView view = new ActivityFavoriteStateView();
        view.setFavorited(Boolean.TRUE);
        when(v1ActivityFavoriteService.getFavoriteState(8L, 2L, UserRole.STUDENT)).thenReturn(view);

        mockMvc.perform(get("/api/v1/activities/8/favorite")
                        .param("operatorUserId", "2")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("favorite state"))
                .andExpect(jsonPath("$.data.favorited").value(true));
    }

    @Test
    void favoriteActivity_shouldReturnUnifiedSuccessBody() throws Exception {
        ActivityFavoriteStateView view = new ActivityFavoriteStateView();
        view.setFavorited(Boolean.TRUE);
        when(v1ActivityFavoriteService.favoriteActivity(8L, 2L, UserRole.STUDENT)).thenReturn(view);

        mockMvc.perform(post("/api/v1/activities/8/favorite")
                        .param("operatorUserId", "2")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("activity favorited"))
                .andExpect(jsonPath("$.data.favorited").value(true));
    }

    @Test
    void unfavoriteActivity_shouldReturnUnifiedSuccessBody() throws Exception {
        ActivityFavoriteStateView view = new ActivityFavoriteStateView();
        view.setFavorited(Boolean.FALSE);
        when(v1ActivityFavoriteService.unfavoriteActivity(8L, 2L, UserRole.STUDENT)).thenReturn(view);

        mockMvc.perform(delete("/api/v1/activities/8/favorite")
                        .param("operatorUserId", "2")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("activity unfavorited"))
                .andExpect(jsonPath("$.data.favorited").value(false));
    }

    @Test
    void getUserFavorites_shouldReturnUnifiedSuccessBody() throws Exception {
        FavoriteActivityView view = new FavoriteActivityView();
        view.setActivityId(8L);
        view.setTitle("Campus Hackday");
        when(v1ActivityFavoriteService.getUserFavorites(12L, 12L, UserRole.STUDENT)).thenReturn(List.of(view));

        mockMvc.perform(get("/api/v1/users/12/favorites")
                        .param("operatorUserId", "12")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].activityId").value(8))
                .andExpect(jsonPath("$.data[0].title").value("Campus Hackday"));
    }

    @Test
    void getFavoriteState_shouldValidateOperatorUserId() throws Exception {
        mockMvc.perform(get("/api/v1/activities/8/favorite")
                        .param("operatorUserId", "0")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void getUserFavorites_shouldReturnForbiddenWhenRoleDenied() throws Exception {
        when(v1ActivityFavoriteService.getUserFavorites(12L, 2L, UserRole.STUDENT))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "only self or admin can access this resource"));

        mockMvc.perform(get("/api/v1/users/12/favorites")
                        .param("operatorUserId", "2")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
    }

    @Test
    void favoriteActivity_shouldReturnNotFoundWhenActivityHidden() throws Exception {
        when(v1ActivityFavoriteService.favoriteActivity(8L, 2L, UserRole.STUDENT))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "activity is not publicly visible"));

        mockMvc.perform(post("/api/v1/activities/8/favorite")
                        .param("operatorUserId", "2")
                        .param("operatorRole", "STUDENT"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }
}
