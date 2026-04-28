package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityManagerPermissionGrant;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.User;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityManagerPermission;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.BasicStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityManagerPermissionGrantMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.service.impl.v1.V1PublicActivityServiceImpl;
import com.campus.activity.service.v1.ActivityPhaseResolver;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.view.v1.ManageableActivityView;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class V1PublicActivityServiceImplTest {

    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivityReviewMapper reviewMapper;

    @Mock
    private ActivityManagerPermissionGrantMapper managerPermissionGrantMapper;

    @Mock
    private OperatorPermissionService permissionService;

    private final ActivityPhaseResolver activityPhaseResolver = new ActivityPhaseResolver();

    private V1PublicActivityServiceImpl publicActivityService;

    @BeforeEach
    void setUp() {
        publicActivityService = new V1PublicActivityServiceImpl(
                activityMapper,
                reviewMapper,
                managerPermissionGrantMapper,
                permissionService,
                activityPhaseResolver
        );
    }

    @Test
    void listPublicActivities_shouldReturnResolvedStatusForApprovedRows() {
        ActivityListItemView approved = new ActivityListItemView();
        approved.setId(7L);
        approved.setStatus(ActivityStatus.PUBLISHED);
        approved.setStartTime(LocalDateTime.now().plusDays(1));
        approved.setEndTime(LocalDateTime.now().plusDays(2));
        approved.setRegistrationDeadline(LocalDateTime.now().minusHours(1));
        approved.setReviewStatus(ReviewStatus.APPROVED);

        ActivityListItemView rejected = new ActivityListItemView();
        rejected.setId(8L);
        rejected.setStatus(ActivityStatus.PUBLISHED);
        rejected.setStartTime(LocalDateTime.now().plusDays(1));
        rejected.setEndTime(LocalDateTime.now().plusDays(2));
        rejected.setRegistrationDeadline(LocalDateTime.now().plusHours(2));
        rejected.setReviewStatus(ReviewStatus.REJECTED);

        when(activityMapper.selectActivityList(null, null, null, null, null, null))
                .thenReturn(List.of(approved, rejected));

        List<ActivityListItemView> rows = publicActivityService.listPublicActivities(null, null, null);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getId()).isEqualTo(7L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ActivityStatus.REGISTRATION_CLOSED);
    }

    @Test
    void getPublicActivityDetail_shouldReturnResolvedStatus() {
        Activity activity = new Activity();
        activity.setId(7L);
        activity.setStatus(ActivityStatus.PUBLISHED);
        activity.setStartTime(LocalDateTime.now().minusHours(2));
        activity.setEndTime(LocalDateTime.now().plusHours(2));

        ActivityReview review = new ActivityReview();
        review.setActivityId(7L);
        review.setReviewStatus(ReviewStatus.APPROVED);

        when(activityMapper.selectById(7L)).thenReturn(activity);
        when(reviewMapper.selectById(7L)).thenReturn(review);

        Activity result = publicActivityService.getPublicActivityDetail(7L);

        assertThat(result.getStatus()).isEqualTo(ActivityStatus.ONGOING);
    }

    @Test
    void listManageableActivities_shouldMergeOrganizerOwnAndGrantedActivities() {
        User operator = new User();
        operator.setId(11L);
        operator.setRole(UserRole.ORGANIZER);
        when(permissionService.verifyOperator(11L, UserRole.ORGANIZER)).thenReturn(operator);

        Activity own = new Activity();
        own.setId(101L);
        own.setOrganizerId(11L);
        own.setTitle("own");
        own.setStatus(ActivityStatus.PUBLISHED);
        own.setStartTime(LocalDateTime.now().plusDays(1));
        own.setEndTime(LocalDateTime.now().plusDays(1).plusHours(1));
        when(activityMapper.selectList(any())).thenReturn(List.of(own));

        ActivityManagerPermissionGrant grant = new ActivityManagerPermissionGrant();
        grant.setActivityId(102L);
        grant.setStatus(BasicStatus.ACTIVE);
        grant.setPermissions("CHECK_IN");
        when(managerPermissionGrantMapper.selectList(any())).thenReturn(List.of(grant));

        Activity granted = new Activity();
        granted.setId(102L);
        granted.setOrganizerId(20L);
        granted.setTitle("granted");
        granted.setStatus(ActivityStatus.PUBLISHED);
        granted.setStartTime(LocalDateTime.now().plusDays(2));
        granted.setEndTime(LocalDateTime.now().plusDays(2).plusHours(1));
        when(activityMapper.selectBatchIds(any())).thenReturn(List.of(own, granted));

        List<ManageableActivityView> views = publicActivityService.listManageableActivities(11L, UserRole.ORGANIZER);

        assertThat(views).hasSize(2);
        ManageableActivityView ownView = views.stream().filter(item -> Long.valueOf(101L).equals(item.getId())).findFirst().orElse(null);
        ManageableActivityView grantedView = views.stream().filter(item -> Long.valueOf(102L).equals(item.getId())).findFirst().orElse(null);
        assertThat(ownView).isNotNull();
        assertThat(ownView.getIsOrganizer()).isTrue();
        assertThat(ownView.getCanCheckIn()).isTrue();
        assertThat(ownView.getCanViewRegistrations()).isTrue();
        assertThat(grantedView).isNotNull();
        assertThat(grantedView.getIsOrganizer()).isFalse();
        assertThat(grantedView.getCanCheckIn()).isTrue();
    }

    @Test
    void listManageableActivities_shouldReturnGrantedActivitiesForStudentOperator() {
        User operator = new User();
        operator.setId(30L);
        operator.setRole(UserRole.STUDENT);
        when(permissionService.verifyOperator(30L, UserRole.STUDENT)).thenReturn(operator);

        ActivityManagerPermissionGrant grant = new ActivityManagerPermissionGrant();
        grant.setActivityId(201L);
        grant.setStatus(BasicStatus.ACTIVE);
        grant.setPermissions(ActivityManagerPermission.VIEW_REGISTRATIONS.name());
        when(managerPermissionGrantMapper.selectList(any())).thenReturn(List.of(grant));

        Activity granted = new Activity();
        granted.setId(201L);
        granted.setOrganizerId(99L);
        granted.setTitle("student granted");
        granted.setStatus(ActivityStatus.PUBLISHED);
        granted.setStartTime(LocalDateTime.now().plusDays(3));
        granted.setEndTime(LocalDateTime.now().plusDays(3).plusHours(1));
        when(activityMapper.selectBatchIds(any())).thenReturn(List.of(granted));

        List<ManageableActivityView> views = publicActivityService.listManageableActivities(30L, UserRole.STUDENT);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).getId()).isEqualTo(201L);
        assertThat(views.get(0).getCanViewRegistrations()).isTrue();
        assertThat(views.get(0).getCanCheckIn()).isFalse();
    }
}
