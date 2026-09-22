package com.javajambs.cher.post;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import jakarta.servlet.ServletException;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;

import com.javajambs.cher.image.ImageService;
import com.javajambs.cher.user.User;

@WebMvcTest(controllers = PostController.class, excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(PostControllerTest.NoOpViewResolverConfig.class)
public class PostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PostRepository repository;

    @MockitoBean
    private ImageService imageService;

    @TestConfiguration
    static class NoOpViewResolverConfig {
        @Bean
        ViewResolver viewResolver() {
            return (viewName, locale) -> viewName.startsWith("redirect:")
                    ? new RedirectView(viewName.substring("redirect:".length()))
                    : (model, request, response) -> {
                    };
        }
    }

    @AfterEach
    void clearSecurityContext() {
        TestSecurityContextHolder.clearContext();
    }

    private static User authenticatedUser() {
        User user = new User();
        user.setUsername("byron");
        user.setEmail("byron@example.com");
    
    TestSecurityContextHolder.setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "password", user.getAuthorities()));

        return user;
    }

    @Test
    void indexPage_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/posts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(repository, never()).findAll();
    }

    @Test
    void indexPage_showsAllPostsWhenAuthenticated() throws Exception {
        User user = authenticatedUser();
        List<Post> posts = List.of(new Post("Sunset walk", user));
        when(repository.findAll()).thenReturn(posts);

        mockMvc.perform(get("/posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("posts"))
                .andExpect(model().attribute("posts", posts))
                .andExpect(model().attributeExists("post"));
    }

    @Test
    void createPost_withoutImage_savesAndRedirectsToPosts() throws Exception {
        User user = authenticatedUser();

        mockMvc.perform(post("/posts").param("caption", "Hello"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        verify(repository).save(argThat(saved ->
                "Hello".equals(saved.getCaption())
                        && user.equals(saved.getUser())
                        && saved.getCreatedAt() != null));
        verify(imageService, never()).uploadImage(any(), any());
    }

    @Test
    void createPost_withValidImage_uploadsAndSetsImageUrl() throws Exception {
        authenticatedUser();
        MockMultipartFile image = new MockMultipartFile("image", "sunset.png", "image/png", "bytes".getBytes());
        when(imageService.uploadImage(eq("posts"), any())).thenReturn("generated-name.png");

        mockMvc.perform(multipart("/posts")
                .file(image)
                .param("caption", "Hello")
                .with(request -> {
                    request.setMethod("POST");
                    return request;
                }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        verify(repository).save(argThat(saved ->
                "/img/posts/generated-name.png".equals(saved.getImageUrl())));
    }
   
    @Test
    void createPost_withNonImageFile_showsErrorAndDoesNotSave() throws Exception {
        authenticatedUser();
        MockMultipartFile file = new MockMultipartFile("image", "doc.pdf", "application/pdf", "bytes".getBytes());

        mockMvc.perform(multipart("/posts")
                .file(file)
                .param("caption", "Hello")
                .with(request -> {
                    request.setMethod("POST");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(view().name("posts"))
                .andExpect(model().attributeExists("postError"));

        verify(repository, never()).save(any());
        verify(imageService, never()).uploadImage(any(), any());
    }

    @Test
    void createPost_whenImageServiceThrows_showsErrorAndDoesNotSave() throws Exception {
        authenticatedUser();
        MockMultipartFile image = new MockMultipartFile("image", "sunset.png", "image/png", "bytes".getBytes());
        when(imageService.uploadImage(any(), any())).thenThrow(new IOException("disk full"));

        mockMvc.perform(multipart("/posts")
                .file(image)
                .param("caption", "Hello")
                .with(request -> {
                    request.setMethod("POST");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(view().name("posts"))
                .andExpect(model().attributeExists("postError"));

        verify(repository, never()).save(any());
    }

    @Test
    void editPage_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/posts/1/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(repository, never()).findById(any());
    }

    @Test
    void editPage_returnsEditViewWithPostWhenAuthenticated() throws Exception {
        User user = authenticatedUser();
        Post post = new Post("Original caption", user);
        when(repository.findById(1L)).thenReturn(Optional.of(post));

        mockMvc.perform(get("/posts/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-posts"))
                .andExpect(model().attribute("post", post));
    }

    @Test
    void editPage_whenPostNotFound_propagatesException() {
        authenticatedUser();
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ServletException thrown = assertThrows(ServletException.class,
                () -> mockMvc.perform(get("/posts/99/edit")));
        assertInstanceOf(NoSuchElementException.class, thrown.getCause());
    }

    // Note: PostController#update has no @AuthenticationPrincipal / login
    // check, unlike index/create/edit, so there's no "redirects to login"
    // test for it here.
    @Test
    void updatePost_updatesCaptionAndRedirectsToPosts() throws Exception {
        User user = authenticatedUser();
        Post existingPost = new Post("Old caption", user);
        when(repository.findById(1L)).thenReturn(Optional.of(existingPost));

        mockMvc.perform(post("/posts/1/edit").param("caption", "New caption"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        verify(repository).save(argThat(saved -> "New caption".equals(saved.getCaption())));
    }

    @Test
    void updatePost_withValidImage_uploadsAndSetsImageUrl() throws Exception {
        User user = authenticatedUser();
        Post existingPost = new Post("Old caption", user);
        when(repository.findById(1L)).thenReturn(Optional.of(existingPost));
        MockMultipartFile image = new MockMultipartFile("image", "new.png", "image/png", "bytes".getBytes());
        when(imageService.uploadImage(eq("posts"), any())).thenReturn("new-name.png");

        mockMvc.perform(multipart("/posts/1/edit")
                .file(image)
                .param("caption", "New caption")
                .with(request -> {
                    request.setMethod("POST");
                    return request;
                }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        verify(repository).save(argThat(saved ->
                "/img/posts/new-name.png".equals(saved.getImageUrl())));
    }

    @Test
    void updatePost_withNonImageFile_showsErrorAndDoesNotSave() throws Exception {
        User user = authenticatedUser();
        Post existingPost = new Post("Old caption", user);
        when(repository.findById(1L)).thenReturn(Optional.of(existingPost));
        MockMultipartFile file = new MockMultipartFile("image", "doc.pdf", "application/pdf", "bytes".getBytes());

        mockMvc.perform(multipart("/posts/1/edit")
                .file(file)
                .param("caption", "New caption")
                .with(request -> {
                    request.setMethod("POST");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-posts"))
                .andExpect(model().attributeExists("postError"));

        verify(repository, never()).save(any());
    }

    @Test
    void updatePost_whenPostNotFound_propagatesException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ServletException thrown = assertThrows(ServletException.class,
                () -> mockMvc.perform(post("/posts/99/edit").param("caption", "New caption")));
        assertInstanceOf(NoSuchElementException.class, thrown.getCause());
    }

    @Test
    void deletePost_deletesAndRedirectsToPosts() throws Exception {
        User user = authenticatedUser();
        Post post = new Post("Caption", user);
        when(repository.findById(1L)).thenReturn(Optional.of(post));

        mockMvc.perform(post("/posts/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/posts"));

        verify(repository).delete(post);
    }

    @Test
    void deletePost_whenPostNotFound_propagatesException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        ServletException thrown = assertThrows(ServletException.class,
                () -> mockMvc.perform(post("/posts/99/delete")));
        assertInstanceOf(NoSuchElementException.class, thrown.getCause());
    }

}
