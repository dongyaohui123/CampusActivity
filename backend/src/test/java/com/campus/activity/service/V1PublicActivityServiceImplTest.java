package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.campus.activity.entity.Activity;
import com.campus.activity.entity.ActivityReview;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.ReviewStatus;
import com.campus.activity.enums.Visibility;
import com.campus.activity.mapper.ActivityMapper;
import com.campus.activity.mapper.ActivityReviewMapper;
import com.campus.activity.service.impl.v1.V1PublicActivityServiceImpl;
import com.campus.activity.service.v1.ActivityPhaseResolver;
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

    private final ActivityPhaseResolver activityPhaseResolver = new ActivityPhaseResolver();

    private V1PublicActivityServiceImpl publicActivityService;

    @BeforeEach
    void setUp() {
        publicActivityService = new V1PublicActivityServiceImpl(
                activityMapper,
                reviewMapper,
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

        when(activityMapper.selectActivityList(null, Visibility.PUBLIC, null, null, null, null))
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
        activity.setVisibility(Visibility.PUBLIC);
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
}
