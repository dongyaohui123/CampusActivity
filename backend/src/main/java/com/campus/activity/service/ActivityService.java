package com.campus.activity.service;

import com.campus.activity.dto.activity.CancelActivityRequest;
import com.campus.activity.dto.activity.CreateActivityRequest;
import com.campus.activity.dto.activity.UpdateActivityRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.ActivityStatus;
import com.campus.activity.enums.Visibility;
import java.time.LocalDateTime;
import java.util.List;

public interface ActivityService {
    Activity createActivity(CreateActivityRequest request);

    Activity updateActivity(Long id, UpdateActivityRequest request);

    void cancelActivity(Long id, CancelActivityRequest request);

    Activity getActivityById(Long id);

    List<ActivityListItemView> listActivities(
            ActivityStatus status,
            Visibility visibility,
            Long organizerId,
            String keyword,
            LocalDateTime startFrom,
            LocalDateTime startTo
    );
}
