package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.campus.activity.config.UploadProperties;
import com.campus.activity.service.impl.v1.AvatarUrlServiceImpl;
import org.junit.jupiter.api.Test;

class AvatarUrlServiceImplTest {
    @Test
    void toPublicUrl_shouldPrefixRelativeAvatarPath() {
        UploadProperties props = new UploadProperties();
        props.setAvatarUrlPrefix("/static/avatars");
        props.setPublicBaseUrl("http://127.0.0.1:8080/");
        AvatarUrlServiceImpl service = new AvatarUrlServiceImpl(props);

        String result = service.toPublicUrl("/static/avatars/u_9_123.png");

        assertThat(result).isEqualTo("http://127.0.0.1:8080/static/avatars/u_9_123.png");
    }

    @Test
    void normalizeForStorage_shouldStripConfiguredBaseUrlForLocalAvatar() {
        UploadProperties props = new UploadProperties();
        props.setAvatarUrlPrefix("/static/avatars");
        props.setPublicBaseUrl("http://127.0.0.1:8080");
        AvatarUrlServiceImpl service = new AvatarUrlServiceImpl(props);

        String result = service.normalizeForStorage("http://127.0.0.1:8080/static/avatars/u_9_123.png");

        assertThat(result).isEqualTo("/static/avatars/u_9_123.png");
    }

    @Test
    void normalizeForStorage_shouldKeepExternalAbsoluteUrl() {
        UploadProperties props = new UploadProperties();
        props.setAvatarUrlPrefix("/static/avatars");
        props.setPublicBaseUrl("http://127.0.0.1:8080");
        AvatarUrlServiceImpl service = new AvatarUrlServiceImpl(props);

        String result = service.normalizeForStorage("https://thirdwx.qlogo.cn/mmopen/example/avatar.png");

        assertThat(result).isEqualTo("https://thirdwx.qlogo.cn/mmopen/example/avatar.png");
    }
}
