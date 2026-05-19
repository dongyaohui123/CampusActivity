package com.campus.activity.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campus.activity.common.ErrorCode;
import com.campus.activity.config.UploadProperties;
import com.campus.activity.exception.BusinessException;
import com.campus.activity.service.impl.v1.ActivityCoverStorageServiceImpl;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class ActivityCoverStorageServiceImplTest {
    @TempDir
    Path tempDir;

    @Test
    void storeCover_shouldCreateDirectoryAndReturnPublicUrl() throws Exception {
        UploadProperties props = new UploadProperties();
        Path coverDir = tempDir.resolve("activity-covers");
        props.setActivityCoverDir(coverDir.toString());
        props.setActivityCoverUrlPrefix("/static/activity-covers");
        props.setActivityCoverMaxSizeKb(2048);
        ActivityCoverStorageServiceImpl service = new ActivityCoverStorageServiceImpl(props);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                "png-cover".getBytes(StandardCharsets.UTF_8)
        );

        String url = service.storeCover(7L, file);

        assertThat(url).startsWith("/static/activity-covers/ac_7_").endsWith(".png");
        assertThat(Files.exists(coverDir)).isTrue();

        String fileName = url.substring(url.lastIndexOf('/') + 1);
        assertThat(Files.exists(coverDir.resolve(fileName))).isTrue();
    }

    @Test
    void storeCover_shouldRejectUnsupportedContentType() {
        UploadProperties props = new UploadProperties();
        props.setActivityCoverDir(tempDir.resolve("activity-covers").toString());
        ActivityCoverStorageServiceImpl service = new ActivityCoverStorageServiceImpl(props);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.gif",
                "image/gif",
                new byte[] {1, 2, 3}
        );

        assertThatThrownBy(() -> service.storeCover(8L, file))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }

    @Test
    void storeCover_shouldRejectWhenFileExceedsMaxSize() {
        UploadProperties props = new UploadProperties();
        props.setActivityCoverDir(tempDir.resolve("activity-covers").toString());
        props.setActivityCoverMaxSizeKb(1);
        ActivityCoverStorageServiceImpl service = new ActivityCoverStorageServiceImpl(props);

        byte[] largeContent = new byte[1025];
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.jpg",
                "image/jpeg",
                largeContent
        );

        assertThatThrownBy(() -> service.storeCover(9L, file))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.BAD_REQUEST);
    }
}
