package com.campus.activity.service.impl.v1;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.config.UploadProperties;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.service.v1.AvatarStorageService;
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
 * Local filesystem avatar storage implementation.
 */
@Service
public class AvatarStorageServiceImpl implements AvatarStorageService {
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

    public AvatarStorageServiceImpl(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public String storeAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "avatar file is required");
        }

        String contentType = normalizeContentType(file.getContentType());
        if (!SUPPORTED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "avatar image type must be jpeg/png/webp");
        }

        long maxSizeBytes = resolveMaxSizeBytes(uploadProperties.getAvatarMaxSizeKb());
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException(
                    ErrorCode.BAD_REQUEST,
                    "avatar image size must be <= " + uploadProperties.getAvatarMaxSizeKb() + "KB"
            );
        }

        Path avatarDir = Paths.get(uploadProperties.getAvatarDir()).toAbsolutePath().normalize();
        ensureDirectoryExists(avatarDir);

        String extension = resolveExtension(contentType);
        String fileName = buildFileName(userId, extension);
        Path target = avatarDir.resolve(fileName).normalize();
        if (!target.startsWith(avatarDir)) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "avatar target path is invalid");
        }
        try {
            file.transferTo(target.toFile());
        } catch (IOException | IllegalStateException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "avatar upload failed");
        }

        String normalizedPrefix = normalizePrefix(uploadProperties.getAvatarUrlPrefix());
        return normalizedPrefix + "/" + fileName;
    }

    private long resolveMaxSizeBytes(int maxSizeKb) {
        int safeKb = maxSizeKb > 0 ? maxSizeKb : 1024;
        return safeKb * 1024L;
    }

    private void ensureDirectoryExists(Path dir) {
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "avatar storage is unavailable");
        }
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String resolveExtension(String contentType) {
        return CONTENT_TYPE_EXT.getOrDefault(contentType, ".png");
    }

    private String buildFileName(Long userId, String extension) {
        int random = ThreadLocalRandom.current().nextInt(100000, 1000000);
        return "u_" + userId + "_" + System.currentTimeMillis() + "_" + random + extension;
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
