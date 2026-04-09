package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.registration.RegistrationCancelRequest;
import com.campus.activity.dto.v1.registration.RegistrationCreateRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.User;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.RegistrationStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.service.impl.v1.V1RegistrationServiceImpl;
import com.campus.activity.service.v1.OperatorPermissionService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class V1RegistrationServiceImplTest {
    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivityRegistrationMapper registrationMapper;

    @Mock
    private ActivityReviewMapper reviewMapper;

    @Mock
    private OperatorPermissionService permissionService;

    @InjectMocks
    private V1RegistrationServiceImpl registrationService;

    @Test
    void register_shouldInsertRegistrationWhenActivityApproved() {
        User student = new User();
        student.setId(2L);
        student.setRole(UserRole.STUDENT);
        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(student);

        Activity activity = new Activity();
        activity.setId(7L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setRegistrationDeadline(LocalDateTime.now().plusDays(1));
        when(activityMapper.selectById(7L)).thenReturn(activity);

        ActivityReview review = new ActivityReview();
        review.setActivityId(7L);
        review.setReviewStatus(ReviewStatus.APPROVED);
        when(reviewMapper.selectById(7L)).thenReturn(review);
        when(registrationMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        when(registrationMapper.insert(any(ActivityRegistration.class))).thenAnswer(invocation -> {
            ActivityRegistration arg = invocation.getArgument(0);
            arg.setId(10L);
            return 1;
        });

        RegistrationCreateRequest request = new RegistrationCreateRequest();
        request.setActivityId(7L);
        request.setRemark("join");
        ActivityRegistration created = registrationService.register(request, 2L, UserRole.STUDENT);

        assertThat(created.getId()).isEqualTo(10L);
        assertThat(created.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(created.getUserId()).isEqualTo(2L);

        ArgumentCaptor<ActivityRegistration> captor = ArgumentCaptor.forClass(ActivityRegistration.class);
        verify(registrationMapper).insert(captor.capture());
        assertThat(captor.getValue().getRegisteredAt()).isNotNull();
    }

    @Test
    void cancelRegistration_shouldRejectWhenPermissionServiceDeniesSelfAccess() {
        User student = new User();
        student.setId(2L);
        student.setRole(UserRole.STUDENT);
        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(student);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setId(9L);
        registration.setActivityId(7L);
        registration.setUserId(3L);
        registration.setStatus(RegistrationStatus.REGISTERED);
        when(registrationMapper.selectById(9L)).thenReturn(registration);
        doThrow(new BusinessException(ErrorCode.FORBIDDEN, "only self can access this resource"))
                .when(permissionService).requireSelf(student, 3L);

        assertThatThrownBy(() -> registrationService.cancelRegistration(
                9L, new RegistrationCancelRequest(), 2L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
