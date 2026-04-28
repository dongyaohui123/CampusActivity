package com.campus.activity.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Maps local upload files (avatars/activity covers) to public static paths.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    private final UploadProperties uploadProperties;

    public StaticResourceConfig(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registerResourceHandler(registry, uploadProperties.getAvatarUrlPrefix(), uploadProperties.getAvatarDir(), "/static/avatars");
        registerResourceHandler(
                registry,
                uploadProperties.getActivityCoverUrlPrefix(),
                uploadProperties.getActivityCoverDir(),
                "/static/activity-covers"
        );
    }

    private void registerResourceHandler(ResourceHandlerRegistry registry, String rawPrefix, String rawDir, String defaultPrefix) {
        String normalizedPrefix = normalizePrefix(rawPrefix, defaultPrefix);
        String pattern = normalizedPrefix + "/**";
        Path dir = Paths.get(rawDir).toAbsolutePath().normalize();
        registry.addResourceHandler(pattern).addResourceLocations(dir.toUri().toString());
    }

    private String normalizePrefix(String rawPrefix, String defaultPrefix) {
        String prefix = rawPrefix == null ? "" : rawPrefix.trim();
        if (prefix.isEmpty()) {
            return defaultPrefix;
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
