package com.campus.activity.service.v1;

import org.springframework.web.multipart.MultipartFile;

/**
 * Activity cover storage service.
 */
public interface ActivityCoverStorageService {
    /**
     * Persist cover file and return the public relative URL.
     */
    String storeCover(Long operatorUserId, MultipartFile file);
}
