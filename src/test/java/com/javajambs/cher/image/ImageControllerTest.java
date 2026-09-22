package com.javajambs.cher.image;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(ImageController.class)
@AutoConfigureMockMvc(addFilters = false)
class ImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageService imageService;

    @TempDir
    Path tempDir;

    @Test
    void uploadImage_delegatesToServiceAndReturnsImgTagPointingAtTheImage() throws Exception {
        when(imageService.uploadImage(eq("avatars"), any(MultipartFile.class))).thenReturn("generated.png");

        mockMvc.perform(multipart("/img/upload")
                .file(new MockMultipartFile("image", "cat.png", "image/png", "bytes".getBytes()))
                .param("path", "avatars"))
                .andExpect(status().isOk())
                .andExpect(content().string("<img src=\"/img/avatars/generated.png\" alt=\"Uploaded image\" />"));

        verify(imageService).uploadImage(eq("avatars"), any(MultipartFile.class));
    }

    @Test
    void uploadImage_defaultsPathToMiscWhenNotProvided() throws Exception {
        when(imageService.uploadImage(eq("misc"), any(MultipartFile.class))).thenReturn("generated.png");

        mockMvc.perform(multipart("/img/upload")
                .file(new MockMultipartFile("image", "cat.png", "image/png", "bytes".getBytes())))
                .andExpect(status().isOk())
                .andExpect(content().string("<img src=\"/img/misc/generated.png\" alt=\"Uploaded image\" />"));
    }

    @Test
    void getImage_streamsTheResourceReturnedByTheService() throws Exception {
        Path file = tempDir.resolve("cat.png");
        Files.write(file, "png-bytes".getBytes());
        when(imageService.loadImage("avatars", "cat.png")).thenReturn(new FileSystemResource(file));

        mockMvc.perform(get("/img/avatars/cat.png"))
                .andExpect(status().isOk())
                .andExpect(content().bytes("png-bytes".getBytes()));
    }

    @Test
    void getImage_returnsPngContentType() throws Exception {
        Path file = tempDir.resolve("cat.png");
        Files.write(file, "png-bytes".getBytes());
        when(imageService.loadImage("avatars", "cat.png")).thenReturn(new FileSystemResource(file));

        mockMvc.perform(get("/img/avatars/cat.png"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
    }

    @Test
    void getImage_returnsJpegContentTypeForJpgAndJpeg() throws Exception {
        Path jpgFile = tempDir.resolve("cat.jpg");
        Files.write(jpgFile, "jpg-bytes".getBytes());
        when(imageService.loadImage("avatars", "cat.jpg")).thenReturn(new FileSystemResource(jpgFile));

        mockMvc.perform(get("/img/avatars/cat.jpg"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE));

        Path jpegFile = tempDir.resolve("cat.jpeg");
        Files.write(jpegFile, "jpeg-bytes".getBytes());
        when(imageService.loadImage("avatars", "cat.jpeg")).thenReturn(new FileSystemResource(jpegFile));

        mockMvc.perform(get("/img/avatars/cat.jpeg"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.IMAGE_JPEG_VALUE));
    }

    @Test
    void getImage_returnsGifContentType() throws Exception {
        Path file = tempDir.resolve("cat.gif");
        Files.write(file, "gif-bytes".getBytes());
        when(imageService.loadImage("avatars", "cat.gif")).thenReturn(new FileSystemResource(file));

        mockMvc.perform(get("/img/avatars/cat.gif"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.IMAGE_GIF_VALUE));
    }

    @Test
    void getImage_returnsWebpContentType() throws Exception {
    Path file = tempDir.resolve("cat.webp");
    Files.write(file, "webp-bytes".getBytes());
    when(imageService.loadImage("avatars", "cat.webp")).thenReturn(new FileSystemResource(file));

    mockMvc.perform(get("/img/avatars/cat.webp"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/webp"));
}

    @Test
    void getImage_returnsOctetStreamForUnrecognizedExtension() throws Exception {
        Path file = tempDir.resolve("cat.txt");
        Files.write(file, "text-bytes".getBytes());
        when(imageService.loadImage("avatars", "cat.txt")).thenReturn(new FileSystemResource(file));

        mockMvc.perform(get("/img/avatars/cat.txt"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.APPLICATION_OCTET_STREAM_VALUE));
    }

    @Test
    void getImage_contentTypeMatchingIsCaseInsensitive() throws Exception {
        Path file = tempDir.resolve("CAT.PNG");
        Files.write(file, "png-bytes".getBytes());
        when(imageService.loadImage("avatars", "CAT.PNG")).thenReturn(new FileSystemResource(file));

        mockMvc.perform(get("/img/avatars/CAT.PNG"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", MediaType.IMAGE_PNG_VALUE));
    }

    @Test
    void uploadImage_returnsBadRequestWhenImagePartMissing() throws Exception {
        mockMvc.perform(multipart("/img/upload")
            .param("path", "avatars"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void uploadImage_propagatesIOExceptionFromService() throws Exception {
        when(imageService.uploadImage(eq("avatars"), any(MultipartFile.class)))
            .thenThrow(new IOException("boom"));

        assertThatThrownBy(() -> mockMvc.perform(multipart("/img/upload")
            .file(new MockMultipartFile("image", "cat.png", "image/png", "bytes".getBytes()))
            .param("path", "avatars")))
            .isInstanceOf(IOException.class);
    }

    @Test
    void getImage_propagatesIOExceptionFromService() throws Exception {
        when(imageService.loadImage("avatars", "missing.png")).thenThrow(new IOException("not found"));

        assertThatThrownBy(() -> mockMvc.perform(get("/img/avatars/missing.png")))
            .isInstanceOf(IOException.class);
    }
}
