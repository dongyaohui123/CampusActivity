package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.service.v1.ActivityPhaseResolver;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ActivityPhaseResolverTest {

    private final ActivityPhaseResolver activityPhaseResolver = new ActivityPhaseResolver();

    @Test
    void resolve_shouldHandleUpcomingClosedOngoingFinishedAndTerminalStates() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 27, 12, 0, 0);

        assertThat(activityPhaseResolver.resolve(
                ActivityStatus.PUBLISHED,
                now.plusDays(1),
                now.plusDays(2),
                now.plusHours(2),
                now
        )).isEqualTo(ActivityStatus.REGISTRATION_OPEN);

        assertThat(activityPhaseResolver.resolve(
                ActivityStatus.PUBLISHED,
                now.plusDays(1),
                now.plusDays(2),
                now.minusMinutes(1),
                now
        )).isEqualTo(ActivityStatus.REGISTRATION_CLOSED);

        assertThat(activityPhaseResolver.resolve(
                ActivityStatus.PUBLISHED,
                now.minusMinutes(1),
                now.plusDays(1),
                now.minusDays(1),
                now
        )).isEqualTo(ActivityStatus.ONGOING);

        assertThat(activityPhaseResolver.resolve(
                ActivityStatus.PUBLISHED,
                now.minusDays(2),
                now,
                now.minusDays(3),
                now
        )).isEqualTo(ActivityStatus.FINISHED);

        assertThat(activityPhaseResolver.resolve(
                ActivityStatus.CANCELLED,
                now.plusDays(1),
                now.plusDays(2),
                now.plusHours(2),
                now
        )).isEqualTo(ActivityStatus.CANCELLED);

        assertThat(activityPhaseResolver.resolve(
                ActivityStatus.DRAFT,
                now.plusDays(1),
                now.plusDays(2),
                now.plusHours(2),
                now
        )).isEqualTo(ActivityStatus.DRAFT);
    }
}
