package com.javajambs.cher.clothes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import java.io.IOException;
import java.util.List;

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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;

import com.javajambs.cher.user.User;

/**
 * Thymeleaf is excluded: no "clothes/*" templates exist yet, so real
 * rendering would fail. A ViewResolver stands in for it instead of relying
 * on Spring MVC's default fallback resolver; it still honours "redirect:"
 * view names so real redirect behavior can be asserted.
 */
@WebMvcTest(controllers = ClothesController.class, excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(ClothesControllerTest.NoOpViewResolverConfig.class)
class ClothesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ClothesService clothesService;

    @MockitoBean
    private ClothesRepository clothesRepository;

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
    void wardrobePage_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/clothes"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(clothesService, never()).getWardrobe(any());
    }

    @Test
    void wardrobePage_withoutTypeParam_showsWholeWardrobe() throws Exception {
        User user = authenticatedUser();
        List<Clothes> wardrobe = List.of(new Clothes("Denim jacket", user));
        when(clothesService.getWardrobe(any(User.class))).thenReturn(wardrobe);

        mockMvc.perform(get("/clothes"))
                .andExpect(status().isOk())
                .andExpect(view().name("clothes/index"))
                .andExpect(model().attribute("wardrobe", wardrobe));

        verify(clothesService, never()).getWardrobeByType(any(), any());
    }

    @Test
    void wardrobePage_withTypeParam_filtersByType() throws Exception {
        User user = authenticatedUser();
        List<Clothes> jackets = List.of(new Clothes("Denim jacket", user));
        when(clothesService.getWardrobeByType(any(User.class), eq("jacket"))).thenReturn(jackets);

        mockMvc.perform(get("/clothes").param("type", "jacket"))
                .andExpect(status().isOk())
                .andExpect(view().name("clothes/index"))
                .andExpect(model().attribute("wardrobe", jackets));

        verify(clothesService, never()).getWardrobe(any());
    }

    @Test
    void newClothingItemPage_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/clothes/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void newClothingItemPage_returnsNewViewWhenAuthenticated() throws Exception {
        authenticatedUser();

        mockMvc.perform(get("/clothes/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("clothes/new"));
    }

    @Test
    void newClothingItemPage_withImageUrlParam_putsItOnTheModel() throws Exception {
        authenticatedUser();

        mockMvc.perform(get("/clothes/new").param("imageUrl", "/img/clothes/edited.png"))
                .andExpect(status().isOk())
                .andExpect(view().name("clothes/new"))
                .andExpect(model().attribute("imageUrl", "/img/clothes/edited.png"));
    }

    @Test
    void createClothingItem_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/clothes").param("name", "Denim jacket"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(clothesService, never()).addClothingItem(any(), any(), any(), any(), any());
    }

    @Test
    void createClothingItem_withoutAnImage_savesAndRedirectsToClothes() throws Exception {
        authenticatedUser();

        mockMvc.perform(post("/clothes").param("name", "Denim jacket"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clothes"));

        verify(clothesService).addClothingItem(any(User.class), eq("Denim jacket"), isNull(), isNull(), isNull());
    }

    @Test
    void createClothingItem_withAnImage_passesItThroughToTheService() throws Exception {
        authenticatedUser();
        MockMultipartFile image = new MockMultipartFile("image", "jacket.png", "image/png", "bytes".getBytes());

        mockMvc.perform(multipart("/clothes")
                .file(image)
                .param("name", "Denim jacket")
                .with(request -> {
                    request.setMethod("POST");
                    return request;
                }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clothes"));

        verify(clothesService).addClothingItem(
                any(User.class), eq("Denim jacket"), isNull(), eq((MultipartFile) image), isNull());
    }

    @Test
    void createClothingItem_withTypeAndImageUrl_passesThemThroughToTheService() throws Exception {
        authenticatedUser();

        mockMvc.perform(post("/clothes")
                        .param("name", "Denim jacket")
                        .param("type", "top")
                        .param("imageUrl", "/img/clothes/edited.png"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clothes"));

        verify(clothesService).addClothingItem(
                any(User.class), eq("Denim jacket"), eq("top"), isNull(), eq("/img/clothes/edited.png"));
    }

    @Test
    void createClothingItem_whenServiceRejectsTheUpload_returnsNewViewWithAnError() throws Exception {
        authenticatedUser();
        when(clothesService.addClothingItem(any(), any(), any(), any(), any()))
                .thenThrow(new IOException("Clothing photo must be an image"));

        mockMvc.perform(post("/clothes").param("name", "Denim jacket"))
                .andExpect(status().isOk())
                .andExpect(view().name("clothes/new"))
                .andExpect(model().attributeExists("clothesError"));
    }

    @Test
    void editClothingItemPage_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/clothes/1/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(clothesService, never()).getClothingItemForEdit(any(), any());
    }

    @Test
    void editClothingItemPage_returnsEditViewWhenAuthenticated() throws Exception {
        User user = authenticatedUser();
        Clothes clothes = new Clothes("Denim jacket", user);
        when(clothesService.getClothingItemForEdit(eq(1L), any(User.class))).thenReturn(clothes);

        mockMvc.perform(get("/clothes/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("clothes/edit"))
                .andExpect(model().attribute("clothes", clothes));
    }

    @Test
    void updateClothingItem_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/clothes/1/edit").param("name", "New name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(clothesService, never()).updateClothingItem(any(), any(), any(), any());
    }

    @Test
    void updateClothingItem_withoutAnImage_updatesAndRedirectsToClothes() throws Exception {
        authenticatedUser();

        mockMvc.perform(post("/clothes/1/edit").param("name", "New name"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clothes"));

        verify(clothesService).updateClothingItem(eq(1L), any(User.class), eq("New name"), isNull());
    }

    @Test
    void updateClothingItem_withAnImage_passesItThroughToTheService() throws Exception {
        authenticatedUser();
        MockMultipartFile image = new MockMultipartFile("image", "jacket.png", "image/png", "bytes".getBytes());

        mockMvc.perform(multipart("/clothes/1/edit")
                .file(image)
                .param("name", "New name")
                .with(request -> {
                    request.setMethod("POST");
                    return request;
                }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clothes"));

        verify(clothesService).updateClothingItem(eq(1L), any(User.class), eq("New name"), eq((MultipartFile) image));
    }

    @Test
    void updateClothingItem_whenServiceRejectsTheUpload_returnsEditViewWithAnError() throws Exception {
        authenticatedUser();
        when(clothesService.updateClothingItem(any(), any(), any(), any()))
                .thenThrow(new IOException("Clothing photo must be an image"));

        mockMvc.perform(post("/clothes/1/edit").param("name", "New name"))
                .andExpect(status().isOk())
                .andExpect(view().name("clothes/edit"))
                .andExpect(model().attributeExists("clothesError"));
    }

    @Test
    void deleteClothingItem_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/clothes/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(clothesService, never()).deleteClothingItem(any(), any());
    }

    @Test
    void deleteClothingItem_deletesAndRedirectsToClothesWhenAuthenticated() throws Exception {
        authenticatedUser();

        mockMvc.perform(post("/clothes/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/clothes"));

        verify(clothesService).deleteClothingItem(eq(1L), any(User.class));
    }
}
