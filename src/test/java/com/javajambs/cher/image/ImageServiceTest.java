package com.javajambs.cher.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class ImageServiceTest {

    private ImageService imageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        imageService = new ImageService();
        ReflectionTestUtils.setField(imageService, "uploadDir", tempDir.toString());
    }

    @Test
    void uploadImage_savesFileAndReturnsGeneratedFilenameWithOriginalExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile("image", "cat.png", "image/png", "image-bytes".getBytes());

        String filename = imageService.uploadImage("avatars", file);

        assertThat(filename).endsWith(".png");
        assertThat(tempDir.resolve("avatars").resolve(filename)).exists();
    }

    @Test
    void uploadImage_rejectsPathTraversalInPath() {
        MockMultipartFile file = new MockMultipartFile("image", "cat.png", "image/png", "image-bytes".getBytes());

        assertThatIOException().isThrownBy(() -> imageService.uploadImage("../../evil", file));
    }

    @Test
    void uploadImage_rejectsFileWithNoExtension() {
        MockMultipartFile file = new MockMultipartFile("image", "noextension", "image/png", "image-bytes".getBytes());

        assertThatIOException().isThrownBy(() -> imageService.uploadImage("avatars", file));
    }

    @Test
    void uploadImage_rejectsNullOriginalFilename() {
        MockMultipartFile file = new MockMultipartFile("image", null, "image/png", "image-bytes".getBytes());

        assertThatIOException().isThrownBy(() -> imageService.uploadImage("avatars", file));
    }

    @Test
    void loadImage_returnsReadableResourceForAnUploadedFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile("image", "cat.png", "image/png", "image-bytes".getBytes());
        String filename = imageService.uploadImage("avatars", file);

        Resource resource = imageService.loadImage("avatars", filename);

        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
        assertThat(resource.getContentAsByteArray()).isEqualTo("image-bytes".getBytes());
    }

    @Test
    void loadImage_rejectsPathTraversalInPath() {
        assertThatIOException().isThrownBy(() -> imageService.loadImage("../../etc", "passwd"));
    }

    @Test
    void loadImage_rejectsPathTraversalInFilename() {
        assertThatIOException().isThrownBy(() -> imageService.loadImage("avatars", "../../../etc/passwd"));
    }

    @Test
    void loadImage_throwsWhenFileDoesNotExist() throws IOException {
        Files.createDirectories(tempDir.resolve("avatars"));

        assertThatIOException().isThrownBy(() -> imageService.loadImage("avatars", "does-not-exist.png"));
    }
}
