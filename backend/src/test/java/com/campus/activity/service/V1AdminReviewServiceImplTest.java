package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.review.ReviewApproveRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityAuditLog;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.User;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.AuditAction;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityAuditLogMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.service.impl.v1.V1AdminReviewServiceImpl;
import com.campus.activity.service.v1.OperatorPermissionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class V1AdminReviewServiceImplTest {
    @Mock
    private ActivityReviewMapper reviewMapper;

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivityAuditLogMapper auditLogMapper;

    @Mock
    private OperatorPermissionService permissionService;

    @InjectMocks
    private V1AdminReviewServiceImpl reviewService;

    @Test
    void approve_shouldUpdateReviewAndActivityAndInsertLog() {
        User admin = new User();
        admin.setId(9L);
        admin.setRole(UserRole.ADMIN);
        when(permissionService.verifyOperator(9L, UserRole.ADMIN)).thenReturn(admin);

        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus(ActivityStatus.DRAFT);
        when(activityMapper.selectById(1L)).thenReturn(activity);

        ActivityReview review = new ActivityReview();
        review.setActivityId(1L);
        review.setReviewStatus(ReviewStatus.PENDING);
        when(reviewMapper.selectById(1L)).thenReturn(review);

        ReviewApproveRequest request = new ReviewApproveRequest();
        request.setComment("looks good");

        ActivityReview updated = reviewService.approve(1L, request, 9L, UserRole.ADMIN);

        assertThat(updated.getReviewStatus()).isEqualTo(ReviewStatus.APPROVED);
        assertThat(updated.getReviewerId()).isEqualTo(9L);
        assertThat(updated.getReviewedAt()).isNotNull();

        ArgumentCaptor<Activity> activityCaptor = ArgumentCaptor.forClass(Activity.class);
        verify(activityMapper).updateById(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getStatus()).isEqualTo(ActivityStatus.PUBLISHED);

        ArgumentCaptor<ActivityAuditLog> logCaptor = ArgumentCaptor.forClass(ActivityAuditLog.class);
        verify(auditLogMapper).insert(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo(AuditAction.APPROVE);
        assertThat(logCaptor.getValue().getOperatorId()).isEqualTo(9L);
    }

    @Test
    void approve_shouldRejectWhenReviewNotPending() {
        User admin = new User();
        admin.setId(9L);
        admin.setRole(UserRole.ADMIN);
        when(permissionService.verifyOperator(9L, UserRole.ADMIN)).thenReturn(admin);

        Activity activity = new Activity();
        activity.setId(1L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        when(activityMapper.selectById(1L)).thenReturn(activity);

        ActivityReview review = new ActivityReview();
        review.setActivityId(1L);
        review.setReviewStatus(ReviewStatus.APPROVED);
        when(reviewMapper.selectById(1L)).thenReturn(review);

        assertThatThrownBy(() -> reviewService.approve(1L, new ReviewApproveRequest(), 9L, UserRole.ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }
}
