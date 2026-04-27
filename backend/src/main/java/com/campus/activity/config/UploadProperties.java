package com.campus.activity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Upload settings for user avatars.
 */
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    private String avatarDir = "./uploads/avatars";
    private String avatarUrlPrefix = "/static/avatars";
    private int avatarMaxSizeKb = 1024;
    private String publicBaseUrl = "http://127.0.0.1:8080";

    public String getAvatarDir() {
        return avatarDir;
    }

    public void setAvatarDir(String avatarDir) {
        this.avatarDir = avatarDir;
    }

    public String getAvatarUrlPrefix() {
        return avatarUrlPrefix;
    }

    public void setAvatarUrlPrefix(String avatarUrlPrefix) {
        this.avatarUrlPrefix = avatarUrlPrefix;
    }

    public int getAvatarMaxSizeKb() {
        return avatarMaxSizeKb;
    }

    public void setAvatarMaxSizeKb(int avatarMaxSizeKb) {
        this.avatarMaxSizeKb = avatarMaxSizeKb;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
