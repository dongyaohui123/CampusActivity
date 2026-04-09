package com.campus.activity.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.service.ActivityService;
import com.campus.activity.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ApiControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private ActivityService activityService;

    @Test
    void createUser_shouldReturnUnifiedSuccessBody() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(UserRole.STUDENT);
        user.setStatus(UserStatus.ACTIVE);

        when(userService.createUser(any())).thenReturn(user);

        String body = """
                {
                  "username":"alice",
                  "passwordHash":"hash1"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("user created"))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.username").value("alice"));
    }

    @Test
    void createUser_shouldReturnValidationError() throws Exception {
        String body = """
                {
                  "passwordHash":"hash1"
                }
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void getUser_shouldMapBusinessExceptionToHttpStatus() throws Exception {
        when(userService.getUserById(999L))
                .thenThrow(new BusinessException(ErrorCode.NOT_FOUND, "user not found: 999"));

        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(40400));
    }

    @Test
    void cancelActivity_shouldReturnUnifiedSuccessBody() throws Exception {
        String body = """
                {
                  "cancelReason":"manual cancel"
                }
                """;

        mockMvc.perform(delete("/api/activities/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("activity cancelled"))
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void createActivity_shouldReturnValidationErrorWhenRequiredFieldsMissing() throws Exception {
        String body = """
                {
                  "title":"No organizer"
                }
                """;

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void createActivity_shouldReturnUnifiedSuccessBody() throws Exception {
        Activity activity = new Activity();
        activity.setId(11L);
        activity.setTitle("Tech Share");
        activity.setStartTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        activity.setEndTime(LocalDateTime.of(2026, 5, 1, 12, 0));

        when(activityService.createActivity(any())).thenReturn(activity);

        String body = """
                {
                  "organizerId":2,
                  "title":"Tech Share",
                  "location":"Hall A",
                  "startTime":"2026-05-01T10:00:00",
                  "endTime":"2026-05-01T12:00:00",
                  "maxParticipants":50
                }
                """;

        mockMvc.perform(post("/api/activities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(11))
                .andExpect(jsonPath("$.message").value("activity created"));
    }
}
