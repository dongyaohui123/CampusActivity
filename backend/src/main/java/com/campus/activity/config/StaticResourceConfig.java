package com.campus.activity.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Maps local avatar files to public static paths.
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {
    private final UploadProperties uploadProperties;

    public StaticResourceConfig(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String normalizedPrefix = normalizePrefix(uploadProperties.getAvatarUrlPrefix());
        String pattern = normalizedPrefix + "/**";
        Path avatarDir = Paths.get(uploadProperties.getAvatarDir()).toAbsolutePath().normalize();
        registry.addResourceHandler(pattern)
                .addResourceLocations(avatarDir.toUri().toString());
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
