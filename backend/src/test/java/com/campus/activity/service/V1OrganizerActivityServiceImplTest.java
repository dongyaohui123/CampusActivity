package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.campus.activity.common.ErrorCode;
import com.campus.activity.dto.v1.activity.OrganizerActivityCheckinRequest;
import com.campus.activity.dto.v1.activity.OrganizerActivityCreateRequest;
import com.campus.activity.dto.v1.activity.OrganizerActivityUpdateRequest;
import com.campus.activity.dto.v1.activity.AddActivityManagerRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityCategory;
import com.campus.activity.entity.ActivityManagerPermissionGrant;
import com.campus.activity.entity.ActivityRegistration;
import com.campus.activity.entity.User;
import com.campus.activity.enums.ActivityManagerPermission;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.BasicStatus;
import com.campus.activity.enums.RegistrationStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityAuditLogMapper;
import com.campus.activity.mapper.ActivityCategoryMapper;
import com.campus.activity.mapper.ActivityManagerPermissionGrantMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityRegistrationMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.mapper.LocationCampusMappingMapper;
import com.campus.activity.mapper.UserMapper;
import com.campus.activity.service.impl.v1.V1OrganizerActivityServiceImpl;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.view.v1.CheckinResultView;
import com.campus.activity.view.v1.ActivityManagerView;
import com.campus.activity.view.v1.OrganizerActivityOptionsView;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class V1OrganizerActivityServiceImplTest {

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivityCategoryMapper activityCategoryMapper;

    @Mock
    private ActivityManagerPermissionGrantMapper activityManagerPermissionGrantMapper;

    @Mock
    private ActivityReviewMapper reviewMapper;

    @Mock
    private ActivityAuditLogMapper auditLogMapper;

    @Mock
    private ActivityRegistrationMapper registrationMapper;

    @Mock
    private LocationCampusMappingMapper locationCampusMappingMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OperatorPermissionService permissionService;

    private V1OrganizerActivityServiceImpl organizerActivityService;

    @BeforeEach
    void setUp() {
        organizerActivityService = new V1OrganizerActivityServiceImpl(
                activityMapper,
                activityCategoryMapper,
                activityManagerPermissionGrantMapper,
                reviewMapper,
                auditLogMapper,
                registrationMapper,
                locationCampusMappingMapper,
                userMapper,
                permissionService
        );
    }

    @Test
    void createActivity_shouldPersistCampusAndUpsertLocationMapping() {
        User organizer = organizerUser(11L);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);

        ActivityCategory category = new ActivityCategory();
        category.setId(7L);
        category.setStatus(BasicStatus.ACTIVE);
        when(activityCategoryMapper.selectById(7L)).thenReturn(category);

        when(activityMapper.insert(any(Activity.class))).thenAnswer(invocation -> {
            Activity toInsert = invocation.getArgument(0);
            toInsert.setId(100L);
            return 1;
        });

        OrganizerActivityCreateRequest request = baseCreateRequest();
        request.setCampusCode("SOUTH");
        request.setLocation("大学生活动中心");

        Activity saved = organizerActivityService.createActivity(request, 11L, UserRole.ORGANIZER);

        assertThat(saved.getId()).isEqualTo(100L);
        assertThat(saved.getFeatured()).isFalse();
        assertThat(saved.getLocation()).isEqualTo("大学生活动中心");
        assertThat(saved.getCategoryId()).isEqualTo(7L);
        verify(locationCampusMappingMapper).upsertMapping("大学生活动中心", "SOUTH");
    }

    @Test
    void createActivity_shouldDefaultOnlineLocationWhenCampusIsOnlineAndLocationIsBlank() {
        User organizer = organizerUser(11L);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);

        ActivityCategory category = new ActivityCategory();
        category.setId(7L);
        category.setStatus(BasicStatus.ACTIVE);
        when(activityCategoryMapper.selectById(7L)).thenReturn(category);

        when(activityMapper.insert(any(Activity.class))).thenAnswer(invocation -> {
            Activity toInsert = invocation.getArgument(0);
            toInsert.setId(101L);
            return 1;
        });

        OrganizerActivityCreateRequest request = baseCreateRequest();
        request.setCampusCode("ONLINE");
        request.setLocation("   ");

        Activity saved = organizerActivityService.createActivity(request, 11L, UserRole.ORGANIZER);

        assertThat(saved.getLocation()).isEqualTo("线上");
        verify(locationCampusMappingMapper).upsertMapping("线上", "ONLINE");
    }

    @Test
    void createActivity_shouldRejectBlankLocationForSouthNorthCampus() {
        User organizer = organizerUser(11L);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);

        OrganizerActivityCreateRequest request = baseCreateRequest();
        request.setCampusCode("NORTH");
        request.setLocation(" ");

        assertThatThrownBy(() -> organizerActivityService.createActivity(request, 11L, UserRole.ORGANIZER))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void createActivity_shouldRejectInvalidCampusCode() {
        User organizer = organizerUser(11L);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);

        OrganizerActivityCreateRequest request = baseCreateRequest();
        request.setCampusCode("WEST");
        request.setLocation("未知地点");

        assertThatThrownBy(() -> organizerActivityService.createActivity(request, 11L, UserRole.ORGANIZER))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void updateActivity_shouldReplaceCategoryRelationAndUpsertLocationMapping() {
        User organizer = organizerUser(11L);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);

        Activity existing = new Activity();
        existing.setId(9L);
        existing.setOrganizerId(11L);
        existing.setStatus(ActivityStatus.DRAFT);
        existing.setLocation("图书馆");
        existing.setFeatured(Boolean.TRUE);
        existing.setStartTime(LocalDateTime.now().plusDays(2));
        existing.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        when(activityMapper.selectById(9L)).thenReturn(existing);

        ActivityCategory category = new ActivityCategory();
        category.setId(5L);
        category.setStatus(BasicStatus.ACTIVE);
        when(activityCategoryMapper.selectById(5L)).thenReturn(category);

        OrganizerActivityUpdateRequest request = new OrganizerActivityUpdateRequest();
        request.setCampusCode("SOUTH");
        request.setLocation("操场");
        request.setActivityTypeId(5L);

        organizerActivityService.updateActivity(9L, request, 11L, UserRole.ORGANIZER);

        assertThat(existing.getLocation()).isEqualTo("操场");
        assertThat(existing.getFeatured()).isTrue();
        assertThat(existing.getCategoryId()).isEqualTo(5L);
        verify(activityMapper).updateById(existing);
        verify(locationCampusMappingMapper).upsertMapping("操场", "SOUTH");
    }

    @Test
    void updateActivity_shouldResolveCampusFromCurrentLocationWhenCampusCodeMissing() {
        User organizer = organizerUser(11L);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);

        Activity existing = new Activity();
        existing.setId(9L);
        existing.setOrganizerId(11L);
        existing.setStatus(ActivityStatus.DRAFT);
        existing.setLocation("图书馆");
        existing.setStartTime(LocalDateTime.now().plusDays(2));
        existing.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        when(activityMapper.selectById(9L)).thenReturn(existing);

        OrganizerActivityUpdateRequest request = new OrganizerActivityUpdateRequest();
        request.setLocation("新图书馆");
        when(locationCampusMappingMapper.selectEnabledByLocationName("图书馆")).thenReturn(null);

        organizerActivityService.updateActivity(9L, request, 11L, UserRole.ORGANIZER);

        verify(locationCampusMappingMapper).upsertMapping("新图书馆", "NORTH");
    }

    @Test
    void getActivityOptions_shouldReturnCampusTypesAndLeafTypes() {
        User organizer = organizerUser(11L);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);
        when(locationCampusMappingMapper.selectEnabledCampusCodes()).thenReturn(List.of("SOUTH", "ONLINE"));

        ActivityCategory leafCategory = new ActivityCategory();
        leafCategory.setId(7L);
        leafCategory.setName("算法竞赛");
        leafCategory.setStatus(BasicStatus.ACTIVE);
        when(activityCategoryMapper.selectActiveLeafCategories()).thenReturn(List.of(leafCategory));

        OrganizerActivityOptionsView options = organizerActivityService.getActivityOptions(11L, UserRole.ORGANIZER);

        assertThat(options.getCampusTypes()).containsExactly("SOUTH", "NORTH", "ONLINE");
        assertThat(options.getActivityTypes()).hasSize(1);
        assertThat(options.getActivityTypes().get(0).getId()).isEqualTo(7L);
        assertThat(options.getActivityTypes().get(0).getName()).isEqualTo("算法竞赛");
    }

    @Test
    void getActivityOptions_shouldFallbackToDefaultCampusTypesAndActiveCategories() {
        User organizer = organizerUser(11L);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);
        when(locationCampusMappingMapper.selectEnabledCampusCodes()).thenReturn(Collections.emptyList());
        when(activityCategoryMapper.selectActiveLeafCategories()).thenReturn(Collections.emptyList());

        ActivityCategory category = new ActivityCategory();
        category.setId(2L);
        category.setName("学术讲座");
        category.setStatus(BasicStatus.ACTIVE);
        when(activityCategoryMapper.selectActiveCategories()).thenReturn(List.of(category));

        OrganizerActivityOptionsView options = organizerActivityService.getActivityOptions(11L, UserRole.ORGANIZER);

        assertThat(options.getCampusTypes()).containsExactly("SOUTH", "NORTH", "ONLINE");
        assertThat(options.getActivityTypes()).hasSize(1);
        assertThat(options.getActivityTypes().get(0).getId()).isEqualTo(2L);
    }

    @Test
    void checkInByTicketCode_shouldMarkRegistrationCheckedIn() {
        User organizer = organizerUser(11L);
        when(permissionService.verifyOrganizerOrActivityManager(7L, 11L, UserRole.ORGANIZER,
                com.campus.activity.enums.ActivityManagerPermission.CHECK_IN)).thenReturn(organizer);

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
        User organizer = organizerUser(11L);
        when(permissionService.verifyOrganizerOrActivityManager(7L, 11L, UserRole.ORGANIZER,
                com.campus.activity.enums.ActivityManagerPermission.CHECK_IN)).thenReturn(organizer);

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
        User organizer = organizerUser(11L);
        when(permissionService.verifyOrganizerOrActivityManager(7L, 11L, UserRole.ORGANIZER,
                com.campus.activity.enums.ActivityManagerPermission.CHECK_IN)).thenReturn(organizer);

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
    void checkInByTicketCode_shouldRejectWhenOperatorLacksActivityPermission() {
        OrganizerActivityCheckinRequest request = new OrganizerActivityCheckinRequest();
        request.setTicketCode("TICKET-001");
        when(permissionService.verifyOrganizerOrActivityManager(7L, 11L, UserRole.STUDENT,
                com.campus.activity.enums.ActivityManagerPermission.CHECK_IN))
                .thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "permission denied for this activity"));

        assertThatThrownBy(() -> organizerActivityService.checkInByTicketCode(7L, request, 11L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void addActivityManager_shouldInsertGrantWithDefaultPermissions() {
        User organizer = organizerUser(11L);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);

        Activity activity = new Activity();
        activity.setId(7L);
        activity.setOrganizerId(11L);
        when(activityMapper.selectById(7L)).thenReturn(activity);

        User manager = new User();
        manager.setId(22L);
        manager.setUsername("manager");
        manager.setNickname("manager");
        manager.setStatus(com.campus.activity.enums.UserStatus.ACTIVE);
        when(userMapper.selectOne(any(QueryWrapper.class))).thenReturn(manager);
        when(activityManagerPermissionGrantMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        AddActivityManagerRequest request = new AddActivityManagerRequest();
        request.setUsername("manager");

        ActivityManagerView view = organizerActivityService.addActivityManager(7L, request, 11L, UserRole.ORGANIZER);

        assertThat(view.getUserId()).isEqualTo(22L);
        assertThat(view.getPermissions()).containsExactly(ActivityManagerPermission.CHECK_IN, ActivityManagerPermission.VIEW_REGISTRATIONS);
        ArgumentCaptor<ActivityManagerPermissionGrant> captor = ArgumentCaptor.forClass(ActivityManagerPermissionGrant.class);
        verify(activityManagerPermissionGrantMapper).insert(captor.capture());
        assertThat(captor.getValue().getActivityId()).isEqualTo(7L);
        assertThat(captor.getValue().getPermissions()).isEqualTo("CHECK_IN,VIEW_REGISTRATIONS");
    }

    @Test
    void listActivityManagers_shouldReturnResolvedUserInfoAndPermissions() {
        User organizer = organizerUser(11L);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(organizer);

        Activity activity = new Activity();
        activity.setId(7L);
        activity.setOrganizerId(11L);
        when(activityMapper.selectById(7L)).thenReturn(activity);

        ActivityManagerPermissionGrant grant = new ActivityManagerPermissionGrant();
        grant.setActivityId(7L);
        grant.setUserId(22L);
        grant.setStatus(BasicStatus.ACTIVE);
        grant.setPermissions("VIEW_REGISTRATIONS,CHECK_IN");
        when(activityManagerPermissionGrantMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(grant));

        User manager = new User();
        manager.setId(22L);
        manager.setNickname("manager");
        manager.setPhone("13800138000");
        when(userMapper.selectBatchIds(List.of(22L))).thenReturn(List.of(manager));

        List<ActivityManagerView> views = organizerActivityService.listActivityManagers(7L, 11L, UserRole.ORGANIZER);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).getUserId()).isEqualTo(22L);
        assertThat(views.get(0).getNickname()).isEqualTo("manager");
        assertThat(views.get(0).getPermissions()).containsExactly(ActivityManagerPermission.CHECK_IN, ActivityManagerPermission.VIEW_REGISTRATIONS);
    }

    private OrganizerActivityCreateRequest baseCreateRequest() {
        OrganizerActivityCreateRequest request = new OrganizerActivityCreateRequest();
        request.setTitle("活动A");
        request.setSummary("摘要");
        request.setActivityTypeId(7L);
        request.setStartTime(LocalDateTime.now().plusDays(2));
        request.setEndTime(LocalDateTime.now().plusDays(2).plusHours(2));
        request.setRegistrationDeadline(LocalDateTime.now().plusDays(1));
        request.setMaxParticipants(80);
        return request;
    }

    private static User organizerUser(Long id) {
        User organizer = new User();
        organizer.setId(id);
        organizer.setRole(UserRole.ORGANIZER);
        return organizer;
    }
}
