package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityFavorite;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.User;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.enums.UserStatus;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.mapper.ActivityFavoriteMapper;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.service.impl.v1.V1ActivityFavoriteServiceImpl;
import com.campus.activity.service.v1.ActivityPhaseResolver;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.view.v1.FavoriteActivityView;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class V1ActivityFavoriteServiceImplTest {
    @Mock
    private ActivityMapper activityMapper;

    @Mock
    private ActivityReviewMapper reviewMapper;

    @Mock
    private ActivityFavoriteMapper favoriteMapper;

    @Mock
    private OperatorPermissionService permissionService;

    private final ActivityPhaseResolver activityPhaseResolver = new ActivityPhaseResolver();

    private V1ActivityFavoriteServiceImpl favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new V1ActivityFavoriteServiceImpl(
                activityMapper,
                reviewMapper,
                favoriteMapper,
                permissionService,
                activityPhaseResolver
        );
    }

    @Test
    void favoriteActivity_shouldInsertFavoriteWhenMissing() {
        User operator = buildUser(1L, UserRole.STUDENT);
        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        when(activityMapper.selectById(9L)).thenReturn(buildPublicActivity(9L));
        when(reviewMapper.selectById(9L)).thenReturn(buildApprovedReview(9L));
        when(favoriteMapper.selectOne(any())).thenReturn(null);

        assertThat(favoriteService.favoriteActivity(9L, 1L, UserRole.STUDENT).getFavorited()).isTrue();
        verify(favoriteMapper).insert((ActivityFavorite) any(ActivityFavorite.class));
    }

    @Test
    void favoriteActivity_shouldBeIdempotentWhenAlreadyFavorited() {
        User operator = buildUser(1L, UserRole.STUDENT);
        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        when(activityMapper.selectById(9L)).thenReturn(buildPublicActivity(9L));
        when(reviewMapper.selectById(9L)).thenReturn(buildApprovedReview(9L));
        when(favoriteMapper.selectOne(any())).thenReturn(new ActivityFavorite());

        assertThat(favoriteService.favoriteActivity(9L, 1L, UserRole.STUDENT).getFavorited()).isTrue();
        verify(favoriteMapper, never()).insert((ActivityFavorite) any(ActivityFavorite.class));
    }

    @Test
    void unfavoriteActivity_shouldDeleteFavoriteWhenExists() {
        User operator = buildUser(1L, UserRole.STUDENT);
        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        when(activityMapper.selectById(9L)).thenReturn(buildPublicActivity(9L));
        when(reviewMapper.selectById(9L)).thenReturn(buildApprovedReview(9L));

        assertThat(favoriteService.unfavoriteActivity(9L, 1L, UserRole.STUDENT).getFavorited()).isFalse();
        verify(favoriteMapper).delete(any());
    }

    @Test
    void unfavoriteActivity_shouldBeIdempotentWhenAlreadyRemoved() {
        User operator = buildUser(1L, UserRole.STUDENT);
        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        when(activityMapper.selectById(9L)).thenReturn(buildPublicActivity(9L));
        when(reviewMapper.selectById(9L)).thenReturn(buildApprovedReview(9L));

        assertThat(favoriteService.unfavoriteActivity(9L, 1L, UserRole.STUDENT).getFavorited()).isFalse();
        verify(favoriteMapper).delete(any());
    }

    @Test
    void favoriteActivity_shouldRejectUnavailableActivityStatus() {
        User operator = buildUser(1L, UserRole.STUDENT);
        Activity activity = buildPublicActivity(9L);
        activity.setStatus(ActivityStatus.DRAFT);

        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        when(activityMapper.selectById(9L)).thenReturn(activity);

        assertThatThrownBy(() -> favoriteService.favoriteActivity(9L, 1L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void favoriteActivity_shouldRejectNotApprovedActivity() {
        User operator = buildUser(1L, UserRole.STUDENT);

        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        when(activityMapper.selectById(9L)).thenReturn(buildPublicActivity(9L));
        when(reviewMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> favoriteService.favoriteActivity(9L, 1L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    void getUserFavorites_shouldEnforceSelfOrAdminPermission() {
        User operator = buildUser(1L, UserRole.STUDENT);
        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        org.mockito.Mockito.doThrow(new BusinessException(ErrorCode.FORBIDDEN, "only self or admin can access this resource"))
                .when(permissionService).requireSelfOrAdmin(operator, 2L);

        assertThatThrownBy(() -> favoriteService.getUserFavorites(2L, 1L, UserRole.STUDENT))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    void getUserFavorites_shouldReturnMapperRowsForAuthorizedUser() {
        User operator = buildUser(1L, UserRole.STUDENT);
        FavoriteActivityView view = new FavoriteActivityView();
        view.setActivityId(9L);
        view.setStatus(ActivityStatus.PUBLISHED);
        view.setStartTime(LocalDateTime.now().plusDays(1));
        view.setEndTime(LocalDateTime.now().plusDays(2));
        view.setRegistrationDeadline(LocalDateTime.now().minusHours(1));
        when(permissionService.verifyOperator(1L, UserRole.STUDENT)).thenReturn(operator);
        when(favoriteMapper.selectUserFavoriteActivities(1L)).thenReturn(List.of(view));

        List<FavoriteActivityView> rows = favoriteService.getUserFavorites(1L, 1L, UserRole.STUDENT);
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getActivityId()).isEqualTo(9L);
        assertThat(rows.get(0).getStatus()).isEqualTo(ActivityStatus.REGISTRATION_CLOSED);
    }

    private User buildUser(Long id, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return user;
    }

    private Activity buildPublicActivity(Long id) {
        Activity activity = new Activity();
        activity.setId(id);
        activity.setStatus(ActivityStatus.PUBLISHED);
        return activity;
    }

    private ActivityReview buildApprovedReview(Long activityId) {
        ActivityReview review = new ActivityReview();
        review.setActivityId(activityId);
        review.setReviewStatus(ReviewStatus.APPROVED);
        return review;
    }
}
