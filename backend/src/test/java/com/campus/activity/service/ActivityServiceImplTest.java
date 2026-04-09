package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.activity.CancelActivityRequest;
import com.campus.activity.dto.activity.CreateActivityRequest;
import com.campus.activity.dto.activity.UpdateActivityRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.Visibility;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.service.impl.ActivityServiceImpl;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ActivityServiceImplTest {
    @Mock
    private ActivityMapper activityMapper;

    @InjectMocks
    private ActivityServiceImpl activityService;

    @Test
    void createActivity_shouldSetDraftAndDefaults() {
        CreateActivityRequest request = new CreateActivityRequest();
        request.setOrganizerId(2L);
        request.setTitle("Tech Talk");
        request.setLocation("Hall A");
        request.setStartTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 1, 12, 0));
        request.setMaxParticipants(100);

        when(activityMapper.insert(any(Activity.class))).thenAnswer(invocation -> {
            Activity arg = invocation.getArgument(0);
            arg.setId(10L);
            return 1;
        });

        Activity created = activityService.createActivity(request);

        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getStatus()).isEqualTo(ActivityStatus.DRAFT);
        assertThat(created.getRegisteredCount()).isEqualTo(0);
        assertThat(created.getVisibility()).isEqualTo(Visibility.PUBLIC);
        assertThat(created.getFeatured()).isFalse();
    }

    @Test
    void createActivity_shouldRejectInvalidTimeRange() {
        CreateActivityRequest request = new CreateActivityRequest();
        request.setOrganizerId(2L);
        request.setTitle("bad time");
        request.setLocation("room");
        request.setStartTime(LocalDateTime.of(2026, 5, 1, 12, 0));
        request.setEndTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        request.setMaxParticipants(10);

        assertThatThrownBy(() -> activityService.createActivity(request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void cancelActivity_shouldSetCancelledFields() {
        Activity existing = new Activity();
        existing.setId(1L);
        existing.setStatus(ActivityStatus.PUBLISHED);
        when(activityMapper.selectById(1L)).thenReturn(existing);
        when(activityMapper.updateById(any(Activity.class))).thenReturn(1);

        CancelActivityRequest request = new CancelActivityRequest();
        request.setCancelReason("weather");
        activityService.cancelActivity(1L, request);

        ArgumentCaptor<Activity> captor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateById(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ActivityStatus.CANCELLED);
        assertThat(captor.getValue().getCancelReason()).isEqualTo("weather");
        assertThat(captor.getValue().getCancelledAt()).isNotNull();
    }

    @Test
    void updateActivity_shouldRejectWhenNoFieldProvided() {
        Activity existing = new Activity();
        existing.setId(5L);
        existing.setStartTime(LocalDateTime.of(2026, 5, 1, 10, 0));
        existing.setEndTime(LocalDateTime.of(2026, 5, 1, 12, 0));
        when(activityMapper.selectById(5L)).thenReturn(existing);

        UpdateActivityRequest request = new UpdateActivityRequest();
        assertThatThrownBy(() -> activityService.updateActivity(5L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
