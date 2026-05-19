package com.campus.activity.service.impl.v1;

import com.campus.activity.config.UploadProperties;
import com.campus.activity.service.v1.AvatarUrlService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Converts avatar paths between stored values and public URLs.
 */
@Service
public class AvatarUrlServiceImpl implements AvatarUrlService {
    private final UploadProperties uploadProperties;

    public AvatarUrlServiceImpl(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public String toPublicUrl(String avatarUrl) {
        String normalizedAvatarUrl = trimToNull(avatarUrl);
        if (normalizedAvatarUrl == null) {
            return avatarUrl;
        }
        if (isAbsoluteUrl(normalizedAvatarUrl)) {
            return normalizedAvatarUrl;
        }

        String normalizedBaseUrl = normalizeBaseUrl(uploadProperties.getPublicBaseUrl());
        if (!StringUtils.hasText(normalizedBaseUrl)) {
            return normalizedAvatarUrl;
        }
        if (normalizedAvatarUrl.startsWith("/")) {
            return normalizedBaseUrl + normalizedAvatarUrl;
        }
        return normalizedBaseUrl + "/" + normalizedAvatarUrl;
    }

    @Override
    public String normalizeForStorage(String avatarUrl) {
        String normalizedAvatarUrl = trimToNull(avatarUrl);
        if (normalizedAvatarUrl == null) {
            return avatarUrl;
        }

        String normalizedBaseUrl = normalizeBaseUrl(uploadProperties.getPublicBaseUrl());
        if (StringUtils.hasText(normalizedBaseUrl) && normalizedAvatarUrl.startsWith(normalizedBaseUrl)) {
            String pathPart = normalizedAvatarUrl.substring(normalizedBaseUrl.length());
            if (pathPart.startsWith(normalizePrefix(uploadProperties.getAvatarUrlPrefix()))) {
                return pathPart;
            }
        }
        return normalizedAvatarUrl;
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

    private String normalizePrefix(String rawPrefix) {
        String prefix = rawPrefix == null ? "" : rawPrefix.trim();
        if (prefix.isEmpty()) {
            return "/static/avatars";
        }
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        while (prefix.endsWith("/") && prefix.length() > 1) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }
}
