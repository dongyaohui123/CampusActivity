package com.campus.activity.service.v1;

import com.campus.activity.dto.v1.activity.CreateActivityCommentRequest;
import com.campus.activity.enums.UserRole;
import com.campus.activity.view.v1.ActivityCommentView;
import java.util.List;

/**
 * Activity comment service.
 */
public interface V1ActivityCommentService {
    /**
     * List comments for a public activity.
     */
    List<ActivityCommentView> listActivityComments(Long activityId);

    /**
     * Create a comment for a public activity.
     */
    ActivityCommentView createComment(Long activityId,
                                      CreateActivityCommentRequest request,
                                      Long operatorUserId,
                                      UserRole operatorRole);

    /**
     * Delete own comment from a public activity.
     */
    void deleteComment(Long activityId, Long commentId, Long operatorUserId, UserRole operatorRole);
}
