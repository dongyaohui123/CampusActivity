package com.campus.activity.service.v1;

import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import java.time.LocalDateTime;
import java.util.List;

public interface V1PublicActivityService {
    List<ActivityListItemView> listPublicActivities(String keyword, LocalDateTime startFrom, LocalDateTime startTo);

    Activity getPublicActivityDetail(Long activityId);
}
