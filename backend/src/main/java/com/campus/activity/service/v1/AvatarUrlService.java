package com.campus.activity.service.v1;

/**
 * Resolves avatar URLs between storage format and public response format.
 */
public interface AvatarUrlService {
    /**
     * Converts a stored avatar value into a public URL for clients.
     *
     * @param avatarUrl stored avatar value
     * @return client-facing avatar URL
     */
    String toPublicUrl(String avatarUrl);

    /**
     * Normalizes a client-provided avatar value before persisting it.
     *
     * @param avatarUrl client-facing avatar URL
     * @return storage-friendly avatar value
     */
    String normalizeForStorage(String avatarUrl);
}
