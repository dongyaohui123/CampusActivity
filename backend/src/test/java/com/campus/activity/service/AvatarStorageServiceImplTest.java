package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.config.UploadProperties;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.service.impl.v1.AvatarStorageServiceImpl;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class AvatarStorageServiceImplTest {
    @TempDir
    Path tempDir;

    @Test
    void storeAvatar_shouldCreateDirectoryAndReturnPublicUrl() throws Exception {
        UploadProperties props = new UploadProperties();
        Path avatarDir = tempDir.resolve("avatars");
        props.setAvatarDir(avatarDir.toString());
        props.setAvatarUrlPrefix("/static/avatars");
        props.setAvatarMaxSizeKb(1024);
        AvatarStorageServiceImpl service = new AvatarStorageServiceImpl(props);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                "png-image".getBytes(StandardCharsets.UTF_8)
        );

        String url = service.storeAvatar(7L, file);

        assertThat(url).startsWith("/static/avatars/u_7_").endsWith(".png");
        assertThat(Files.exists(avatarDir)).isTrue();

        String fileName = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(avatarDir.resolve(fileName))).isTrue();
    }

    @Test
    void storeAvatar_shouldRejectUnsupportedContentType() {
        UploadProperties props = new UploadProperties();
        props.setAvatarDir(tempDir.resolve("avatars").toString());
        AvatarStorageServiceImpl service = new AvatarStorageServiceImpl(props);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.gif",
                "image/gif",
                new byte[] {1, 2, 3}
        );

        assertThatThrownBy(() -> service.storeAvatar(8L, file))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void storeAvatar_shouldRejectWhenFileExceedsMaxSize() {
        UploadProperties props = new UploadProperties();
        props.setAvatarDir(tempDir.resolve("avatars").toString());
        props.setAvatarMaxSizeKb(1);
        AvatarStorageServiceImpl service = new AvatarStorageServiceImpl(props);

        byte[] largeContent = new byte[1025];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                largeContent
        );

        assertThatThrownBy(() -> service.storeAvatar(9L, file))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
