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
import com.campus.activity.service.RegistrationTicketService;
import com.campus.activity.service.impl.v1.V1RegistrationServiceImpl;
import com.campus.activity.service.v1.ActivityPhaseResolver;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.view.v1.TicketDetailView;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

    @Mock
    private RegistrationTicketService registrationTicketService;

    private final ActivityPhaseResolver activityPhaseResolver = new ActivityPhaseResolver();

    private V1RegistrationServiceImpl registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new V1RegistrationServiceImpl(
                activityMapper,
                registrationMapper,
                reviewMapper,
                permissionService,
                registrationTicketService,
                activityPhaseResolver
        );
    }

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
        when(registrationTicketService.generateTicketCode()).thenReturn("TICKET-001");
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
        assertThat(created.getTicketCode()).isEqualTo("TICKET-001");

        ArgumentCaptor<ActivityRegistration> captor = ArgumentCaptor.forClass(ActivityRegistration.class);
        verify(registrationMapper).insert(captor.capture());
        assertThat(captor.getValue().getRegisteredAt()).isNotNull();
        assertThat(captor.getValue().getTicketCode()).isEqualTo("TICKET-001");
        assertThat(captor.getValue().getTicketIssuedAt()).isNotNull();
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

    @Test
    void register_shouldReissueTicketWhenRestoringCancelledRegistration() {
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

        ActivityRegistration existing = new ActivityRegistration();
        existing.setId(10L);
        existing.setActivityId(7L);
        existing.setUserId(2L);
        existing.setStatus(RegistrationStatus.CANCELLED);
        existing.setTicketCode("OLD-CODE");
        when(registrationMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);
        when(registrationTicketService.generateTicketCode()).thenReturn("NEW-CODE");

        RegistrationCreateRequest request = new RegistrationCreateRequest();
        request.setActivityId(7L);
        request.setRemark("restore");
        ActivityRegistration restored = registrationService.register(request, 2L, UserRole.STUDENT);

        assertThat(restored.getStatus()).isEqualTo(RegistrationStatus.REGISTERED);
        assertThat(restored.getTicketCode()).isEqualTo("NEW-CODE");
        assertThat(restored.getTicketIssuedAt()).isNotNull();
    }

    @Test
    void register_shouldRejectWhenRegistrationDeadlineHasPassed() {
        User student = new User();
        student.setId(2L);
        student.setRole(UserRole.STUDENT);
        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(student);

        Activity activity = new Activity();
        activity.setId(7L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setStartTime(LocalDateTime.now().plusDays(1));
        activity.setEndTime(LocalDateTime.now().plusDays(2));
        activity.setRegistrationDeadline(LocalDateTime.now().minusMinutes(1));
        when(activityMapper.selectById(7L)).thenReturn(activity);

        RegistrationCreateRequest request = new RegistrationCreateRequest();
        request.setActivityId(7L);

        assertThatThrownBy(() -> registrationService.register(request, 2L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void register_shouldRejectWhenActivityAlreadyStarted() {
        User student = new User();
        student.setId(2L);
        student.setRole(UserRole.STUDENT);
        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(student);

        Activity activity = new Activity();
        activity.setId(7L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setStartTime(LocalDateTime.now().minusMinutes(5));
        activity.setEndTime(LocalDateTime.now().plusDays(1));
        activity.setRegistrationDeadline(LocalDateTime.now().plusHours(1));
        when(activityMapper.selectById(7L)).thenReturn(activity);

        RegistrationCreateRequest request = new RegistrationCreateRequest();
        request.setActivityId(7L);

        assertThatThrownBy(() -> registrationService.register(request, 2L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void register_shouldRejectWhenActivityFinished() {
        User student = new User();
        student.setId(2L);
        student.setRole(UserRole.STUDENT);
        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(student);

        Activity activity = new Activity();
        activity.setId(7L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setStartTime(LocalDateTime.now().minusDays(2));
        activity.setEndTime(LocalDateTime.now().minusMinutes(1));
        activity.setRegistrationDeadline(LocalDateTime.now().minusDays(3));
        when(activityMapper.selectById(7L)).thenReturn(activity);

        RegistrationCreateRequest request = new RegistrationCreateRequest();
        request.setActivityId(7L);

        assertThatThrownBy(() -> registrationService.register(request, 2L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void cancelRegistration_shouldRejectWhenAlreadyCheckedIn() {
        User student = new User();
        student.setId(2L);
        student.setRole(UserRole.STUDENT);
        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(student);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setId(9L);
        registration.setActivityId(7L);
        registration.setUserId(2L);
        registration.setStatus(RegistrationStatus.CHECKED_IN);
        when(registrationMapper.selectById(9L)).thenReturn(registration);

        assertThatThrownBy(() -> registrationService.cancelRegistration(
                9L, new RegistrationCancelRequest(), 2L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void cancelRegistration_shouldRejectAfterActivityStart() {
        User student = new User();
        student.setId(2L);
        student.setRole(UserRole.STUDENT);
        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(student);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setId(9L);
        registration.setActivityId(7L);
        registration.setUserId(2L);
        registration.setStatus(RegistrationStatus.REGISTERED);
        when(registrationMapper.selectById(9L)).thenReturn(registration);

        Activity activity = new Activity();
        activity.setId(7L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setStartTime(LocalDateTime.now().minusMinutes(10));
        activity.setEndTime(LocalDateTime.now().plusHours(2));
        activity.setRegistrationDeadline(LocalDateTime.now().minusHours(1));
        when(activityMapper.selectById(7L)).thenReturn(activity);

        assertThatThrownBy(() -> registrationService.cancelRegistration(
                9L, new RegistrationCancelRequest(), 2L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void getTicketDetail_shouldIssueTicketForLegacyRegisteredRecord() {
        User student = new User();
        student.setId(2L);
        student.setRole(UserRole.STUDENT);
        when(permissionService.verifyOperator(2L, UserRole.STUDENT)).thenReturn(student);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setId(9L);
        registration.setActivityId(7L);
        registration.setUserId(2L);
        registration.setStatus(RegistrationStatus.REGISTERED);
        when(registrationMapper.selectById(9L)).thenReturn(registration);
        when(registrationTicketService.generateTicketCode()).thenReturn("LEGACY-TICKET");

        Activity activity = new Activity();
        activity.setId(7L);
        activity.setTitle("Campus Hackday");
        when(activityMapper.selectById(7L)).thenReturn(activity);

        TicketDetailView detail = registrationService.getTicketDetail(9L, 2L, UserRole.STUDENT);

        assertThat(detail.getTicketCode()).isEqualTo("LEGACY-TICKET");
        assertThat(detail.getTicketIssuedAt()).isNotNull();
        verify(registrationMapper).updateById(registration);
    }
}
