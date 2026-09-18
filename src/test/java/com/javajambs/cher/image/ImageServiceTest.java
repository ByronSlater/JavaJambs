package com.javajambs.cher.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;

class ImageServiceTest {

    private ImageService imageService;
    private BlobContainerClient containerClient;
    private BlobClient blobClient;

    @BeforeEach
    void setUp() {
        containerClient = mock(BlobContainerClient.class);
        blobClient = mock(BlobClient.class);
        when(containerClient.getBlobClient(anyString())).thenReturn(blobClient);

        imageService = new ImageService(containerClient);
    }

    @Test
    void uploadImage_uploadsFileAndReturnsGeneratedFilenameWithOriginalExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile("image", "cat.png", "image/png", "image-bytes".getBytes());

        String filename = imageService.uploadImage("avatars", file);

        assertThat(filename).endsWith(".png");
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
    void loadImage_returnsReadableResourceForAnExistingBlob() throws IOException {
        when(blobClient.exists()).thenReturn(true);
        when(blobClient.downloadContent()).thenReturn(BinaryData.fromBytes("image-bytes".getBytes()));

        Resource resource = imageService.loadImage("avatars", "cat.png");

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
    void loadImage_throwsWhenBlobDoesNotExist() {
        when(blobClient.exists()).thenReturn(false);

        assertThatIOException().isThrownBy(() -> imageService.loadImage("avatars", "does-not-exist.png"));
    }
}
