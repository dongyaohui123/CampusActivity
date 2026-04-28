package com.campus.activity.service.impl.v1;

import com.campus.activity.config.UploadProperties;
import com.campus.activity.entity.User;
import com.campus.activity.enums.UserRole;
import com.campus.activity.service.v1.ActivityCoverStorageService;
import com.campus.activity.service.v1.OperatorPermissionService;
import com.campus.activity.service.v1.V1OrganizerActivityCoverService;
import com.campus.activity.view.v1.ActivityCoverUploadView;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * Organizer activity cover upload service implementation.
 */
@Service
public class V1OrganizerActivityCoverServiceImpl implements V1OrganizerActivityCoverService {
    private final OperatorPermissionService permissionService;
    private final ActivityCoverStorageService coverStorageService;
    private final UploadProperties uploadProperties;

    public V1OrganizerActivityCoverServiceImpl(
            OperatorPermissionService permissionService,
            ActivityCoverStorageService coverStorageService,
            UploadProperties uploadProperties
    ) {
        this.permissionService = permissionService;
        this.coverStorageService = coverStorageService;
        this.uploadProperties = uploadProperties;
    }

    @Override
    public ActivityCoverUploadView uploadCover(MultipartFile file, Long operatorUserId, UserRole operatorRole) {
        User operator = permissionService.verifyOperator(operatorUserId, operatorRole);
        permissionService.requireRole(operator, UserRole.ORGANIZER);

        String relativeUrl = coverStorageService.storeCover(operator.getId(), file);
        ActivityCoverUploadView view = new ActivityCoverUploadView();
        view.setCoverUrl(toPublicUrl(relativeUrl));
        return view;
    }

    private String toPublicUrl(String rawUrl) {
        String normalizedUrl = trimToNull(rawUrl);
        if (normalizedUrl == null) {
            return rawUrl;
        }
        if (isAbsoluteUrl(normalizedUrl)) {
            return normalizedUrl;
        }

        String baseUrl = normalizeBaseUrl(uploadProperties.getPublicBaseUrl());
        if (!StringUtils.hasText(baseUrl)) {
            return normalizedUrl;
        }
        if (normalizedUrl.startsWith("/")) {
            return baseUrl + normalizedUrl;
        }
        return baseUrl + "/" + normalizedUrl;
    }

    private String trimToNull(String value) {
        String trimmed = StringUtils.trimWhitespace(value);
        return StringUtils.hasText(trimmed) ? trimmed : null;
    }

    private boolean isAbsoluteUrl(String value) {
        return value.startsWith("http://") || value.startsWith("https://");
    }

    private String normalizeBaseUrl(String rawBaseUrl) {
        String baseUrl = rawBaseUrl == null ? "" : rawBaseUrl.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
