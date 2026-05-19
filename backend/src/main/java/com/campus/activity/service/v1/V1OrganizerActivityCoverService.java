package com.campus.activity.service.v1;

import com.campus.activity.enums.UserRole;
import com.campus.activity.view.v1.ActivityCoverUploadView;
import org.springframework.web.multipart.MultipartFile;

/**
 * Organizer activity cover upload service.
 */
public interface V1OrganizerActivityCoverService {
    /**
     * Upload one cover image for organizer activity publishing.
     */
    ActivityCoverUploadView uploadCover(MultipartFile file, Long operatorUserId, UserRole operatorRole);
}
