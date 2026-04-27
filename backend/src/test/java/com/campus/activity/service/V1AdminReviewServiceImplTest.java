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
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.impl.v1.V1AdminReviewServiceImpl;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.view.v1.PendingReviewActivityView;
import java.util.List;
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
    private UserMapper userMapper;

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

    @Test
    void listPendingReviews_shouldUseNicknameThenUsernameThenFallback() {
        User admin = new User();
        admin.setId(9L);
        admin.setRole(UserRole.ADMIN);
        when(permissionService.verifyOperator(9L, UserRole.ADMIN)).thenReturn(admin);

        ActivityReview review1 = new ActivityReview();
        review1.setActivityId(1L);
        review1.setReviewStatus(ReviewStatus.PENDING);

        ActivityReview review2 = new ActivityReview();
        review2.setActivityId(2L);
        review2.setReviewStatus(ReviewStatus.PENDING);

        ActivityReview review3 = new ActivityReview();
        review3.setActivityId(3L);
        review3.setReviewStatus(ReviewStatus.PENDING);

        when(reviewMapper.selectList(any())).thenReturn(List.of(review1, review2, review3));

        Activity activity1 = new Activity();
        activity1.setId(1L);
        activity1.setTitle("活动一");
        activity1.setOrganizerId(101L);

        Activity activity2 = new Activity();
        activity2.setId(2L);
        activity2.setTitle("活动二");
        activity2.setOrganizerId(102L);

        Activity activity3 = new Activity();
        activity3.setId(3L);
        activity3.setTitle("活动三");
        activity3.setOrganizerId(103L);

        when(activityMapper.selectBatchIds(any())).thenReturn(List.of(activity1, activity2, activity3));

        User organizer1 = new User();
        organizer1.setId(101L);
        organizer1.setNickname("组织者甲");
        organizer1.setUsername("org_a");

        User organizer2 = new User();
        organizer2.setId(102L);
        organizer2.setNickname("   ");
        organizer2.setUsername("org_b");

        when(userMapper.selectBatchIds(any())).thenReturn(List.of(organizer1, organizer2));

        List<PendingReviewActivityView> result = reviewService.listPendingReviews(9L, UserRole.ADMIN);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getOrganizerName()).isEqualTo("组织者甲");
        assertThat(result.get(1).getOrganizerName()).isEqualTo("org_b");
        assertThat(result.get(2).getOrganizerName()).isEqualTo("-");
    }
}
