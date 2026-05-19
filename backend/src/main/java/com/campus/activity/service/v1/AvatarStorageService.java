package com.campus.activity.service.v1;

import org.springframework.web.multipart.MultipartFile;

/**
 * Stores user avatars and returns public URLs.
 */
public interface AvatarStorageService {
    /**
     * Persist avatar file and return the public URL.
     */
    String storeAvatar(Long userId, MultipartFile file);
}
