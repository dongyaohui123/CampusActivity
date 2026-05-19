package com.campus.activity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Upload settings for user avatars and activity covers.
 */
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {
    private String avatarDir = "./uploads/avatars";
    private String avatarUrlPrefix = "/static/avatars";
    private int avatarMaxSizeKb = 1024;
    private String activityCoverDir = "./uploads/activity-covers";
    private String activityCoverUrlPrefix = "/static/activity-covers";
    private int activityCoverMaxSizeKb = 2048;
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

    public String getActivityCoverDir() {
        return activityCoverDir;
    }

    public void setActivityCoverDir(String activityCoverDir) {
        this.activityCoverDir = activityCoverDir;
    }

    public String getActivityCoverUrlPrefix() {
        return activityCoverUrlPrefix;
    }

    public void setActivityCoverUrlPrefix(String activityCoverUrlPrefix) {
        this.activityCoverUrlPrefix = activityCoverUrlPrefix;
    }

    public int getActivityCoverMaxSizeKb() {
        return activityCoverMaxSizeKb;
    }

    public void setActivityCoverMaxSizeKb(int activityCoverMaxSizeKb) {
        this.activityCoverMaxSizeKb = activityCoverMaxSizeKb;
    }

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }
}
