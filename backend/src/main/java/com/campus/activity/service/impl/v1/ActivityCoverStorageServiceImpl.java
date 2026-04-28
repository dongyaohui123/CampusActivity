package com.campus.activity.service.impl.v1;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.config.UploadProperties;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.service.v1.ActivityCoverStorageService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Local filesystem activity cover storage implementation.
 */
@Service
public class ActivityCoverStorageServiceImpl implements ActivityCoverStorageService {
    private static final Set<String> SUPPORTED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );
    private static final Map<String, String> CONTENT_TYPE_EXT = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp"
    );

    private final UploadProperties uploadProperties;

    public ActivityCoverStorageServiceImpl(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public String storeCover(Long operatorUserId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "activity cover file is required");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "activity cover type must be jpeg/png/webp");
        }

        long maxSizeBytes = resolveMaxSizeBytes(uploadProperties.getActivityCoverMaxSizeKb());
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "activity cover size must be <= " + uploadProperties.getActivityCoverMaxSizeKb() + "KB"
            );
        }

        Path coverDir = Paths.get(uploadProperties.getActivityCoverDir()).toAbsolutePath().normalize();
        ensureDirectoryExists(coverDir);

        String extension = resolveExtension(contentType);
        String fileName = buildFileName(operatorUserId, extension);
        Path target = coverDir.resolve(fileName).normalize();
        if (!target.startsWith(coverDir)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "activity cover target path is invalid");
        }
        try {
            file.transferTo(target.toFile());
        } catch (IOException | IllegalStateException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "activity cover upload failed");
        }

        String normalizedPrefix = normalizePrefix(uploadProperties.getActivityCoverUrlPrefix());
        return normalizedPrefix + "/" + fileName;
    }

    private long resolveMaxSizeBytes(int maxSizeKb) {
        int safeKb = maxSizeKb > 0 ? maxSizeKb : 2048;
        return safeKb * 1024L;
    }

    private void ensureDirectoryExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "activity cover storage is unavailable");
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveExtension(String contentType) {
        return CONTENT_TYPE_EXT.getOrDefault(contentType, ".png");
    }

    private String buildFileName(Long operatorUserId, String extension) {
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "ac_" + operatorUserId + "_" + System.currentTimeMillis() + "_" + random + extension;
    }

    private String normalizePrefix(String rawPrefix) {
        String prefix = rawPrefix == null ? "" : rawPrefix.trim();
        if (prefix.isEmpty()) {
            return "/static/activity-covers";
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
