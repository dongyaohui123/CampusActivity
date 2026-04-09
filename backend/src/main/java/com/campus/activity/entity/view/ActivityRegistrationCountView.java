package com.campus.activity.entity.view;

import lombok.Data;

/**
 * Aggregated registration count per activity.
 */
@Data
public class ActivityRegistrationCountView {

    private Long activityId;

    private Long registrationCount;
}
