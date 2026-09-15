package com.javajambs.cher.image;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.core.io.FileSystemResource;
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
}
