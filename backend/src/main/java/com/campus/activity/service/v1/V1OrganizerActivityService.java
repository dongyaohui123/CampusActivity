package com.campus.activity.service.v1;

import com.campus.activity.dto.v1.activity.OrganizerActivityCreateRequest;
import com.campus.activity.dto.v1.activity.OrganizerActivityUpdateRequest;
import com.campus.activity.dto.v1.activity.SubmitReviewRequest;
import com.campus.activity.entity.Activity;
import com.campus.activity.entity.view.ActivityListItemView;
import com.campus.activity.enums.RegistrationStatus;
import com.campus.activity.enums.UserRole;
import com.campus.activity.view.v1.ActivityRegistrationUserView;
import java.time.LocalDateTime;
import java.util.List;

public interface V1OrganizerActivityService {
    Activity createActivity(OrganizerActivityCreateRequest request, Long operatorUserId, UserRole operatorRole);

    Activity updateActivity(Long activityId, OrganizerActivityUpdateRequest request, Long operatorUserId, UserRole operatorRole);

    Activity submitForReview(Long activityId, SubmitReviewRequest request, Long operatorUserId, UserRole operatorRole);

    List<ActivityListItemView> listOwnActivities(Long operatorUserId, UserRole operatorRole, String keyword, LocalDateTime startFrom, LocalDateTime startTo);

    List<ActivityRegistrationUserView> listActivityRegistrations(Long activityId, RegistrationStatus status, Long operatorUserId, UserRole operatorRole);
}
