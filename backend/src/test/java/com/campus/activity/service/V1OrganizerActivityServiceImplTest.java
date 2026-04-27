package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.activity.OrganizerActivityCheckinRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.User;
import com.campus.activity.enums.RegistrationStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityAuditLogMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.impl.v1.V1OrganizerActivityServiceImpl;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.view.v1.CheckinResultView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class V1OrganizerActivityServiceImplTest {

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivityReviewMapper reviewMapper;

    @Mock
    private ActivityAuditLogMapper auditLogMapper;

    @Mock
    private ActivityRegistrationMapper registrationMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OperatorPermissionService permissionService;

    @InjectMocks
    private V1OrganizerActivityServiceImpl organizerActivityService;

    @Test
    void checkInByTicketCode_shouldMarkRegistrationCheckedIn() {
        User organizer = new User();
        organizer.setId(11L);
        organizer.setRole(UserRole.ORGANIZER);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);

        Activity activity = new Activity();
        activity.setId(7L);
        activity.setOrganizerId(11L);
        when(activityMapper.selectById(7L)).thenReturn(activity);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setId(22L);
        registration.setActivityId(7L);
        registration.setUserId(3L);
        registration.setStatus(RegistrationStatus.REGISTERED);
        when(registrationMapper.selectOne(any(QueryWrapper.class))).thenReturn(registration);

        User attendee = new User();
        attendee.setId(3L);
        attendee.setNickname("Alice");
        when(userMapper.selectById(3L)).thenReturn(attendee);

        OrganizerActivityCheckinRequest request = new OrganizerActivityCheckinRequest();
        request.setTicketCode("TICKET-001");

        CheckinResultView result = organizerActivityService.checkInByTicketCode(7L, request, 11L, UserRole.ORGANIZER);

        assertThat(result.getRegistrationId()).isEqualTo(22L);
        assertThat(result.getStatus()).isEqualTo(RegistrationStatus.CHECKED_IN);
        assertThat(result.getCheckedInAt()).isNotNull();
        assertThat(result.getNickname()).isEqualTo("Alice");
        verify(registrationMapper).updateById(registration);
    }

    @Test
    void checkInByTicketCode_shouldRejectAlreadyCheckedInTicket() {
        User organizer = new User();
        organizer.setId(11L);
        organizer.setRole(UserRole.ORGANIZER);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);

        Activity activity = new Activity();
        activity.setId(7L);
        activity.setOrganizerId(11L);
        when(activityMapper.selectById(7L)).thenReturn(activity);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityId(7L);
        registration.setStatus(RegistrationStatus.CHECKED_IN);
        when(registrationMapper.selectOne(any(QueryWrapper.class))).thenReturn(registration);

        OrganizerActivityCheckinRequest request = new OrganizerActivityCheckinRequest();
        request.setTicketCode("TICKET-001");

        assertThatThrownBy(() -> organizerActivityService.checkInByTicketCode(7L, request, 11L, UserRole.ORGANIZER))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void checkInByTicketCode_shouldRejectWhenTicketBelongsToAnotherActivity() {
        User organizer = new User();
        organizer.setId(11L);
        organizer.setRole(UserRole.ORGANIZER);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);

        Activity activity = new Activity();
        activity.setId(7L);
        activity.setOrganizerId(11L);
        when(activityMapper.selectById(7L)).thenReturn(activity);

        ActivityRegistration registration = new ActivityRegistration();
        registration.setActivityId(8L);
        registration.setStatus(RegistrationStatus.REGISTERED);
        when(registrationMapper.selectOne(any(QueryWrapper.class))).thenReturn(registration);

        OrganizerActivityCheckinRequest request = new OrganizerActivityCheckinRequest();
        request.setTicketCode("TICKET-001");

        assertThatThrownBy(() -> organizerActivityService.checkInByTicketCode(7L, request, 11L, UserRole.ORGANIZER))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void checkInByTicketCode_shouldRejectWhenOrganizerDoesNotOwnActivity() {
        User organizer = new User();
        organizer.setId(11L);
        organizer.setRole(UserRole.ORGANIZER);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);

        Activity activity = new Activity();
        activity.setId(7L);
        activity.setOrganizerId(99L);
        when(activityMapper.selectById(7L)).thenReturn(activity);

        OrganizerActivityCheckinRequest request = new OrganizerActivityCheckinRequest();
        request.setTicketCode("TICKET-001");

        assertThatThrownBy(() -> organizerActivityService.checkInByTicketCode(7L, request, 11L, UserRole.ORGANIZER))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }
}
