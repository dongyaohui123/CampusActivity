package com.campus.activity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.RegistrationStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.service.ActivityService;
import com.campus.activity.service.UserService;
import com.campus.activity.service.v1.V1AdminReviewService;
import com.campus.activity.service.v1.V1AuthService;
import com.campus.activity.service.v1.V1OrganizerActivityService;
import com.campus.activity.service.v1.V1PublicActivityService;
import com.campus.activity.service.v1.V1RegistrationService;
import com.campus.activity.service.v1.V1UserService;
import java.util.List;
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
}
