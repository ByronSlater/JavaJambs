package com.javajambs.cher.outfit;

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

import com.javajambs.cher.clothes.Clothes;
import com.javajambs.cher.clothes.ClothesService;
import com.javajambs.cher.user.User;

@WebMvcTest(controllers = OutfitController.class, excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(OutfitControllerTest.NoOpViewResolverConfig.class)
class OutfitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OutfitService outfitService;

    @MockitoBean
    private ClothesService clothesService;

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
    void outfitsPage_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/outfits"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(outfitService, never()).getOutfits(any());
    }

    @Test
    void outfitsPage_withoutTypeParam_showsAllOutfits() throws Exception {
        User user = authenticatedUser();
        List<Outfit> outfits = List.of(new Outfit("Beach look", user));
        when(outfitService.getOutfits(any(User.class))).thenReturn(outfits);

        mockMvc.perform(get("/outfits"))
                .andExpect(status().isOk())
                .andExpect(view().name("outfit/index"))
                .andExpect(model().attribute("outfits", outfits));

        verify(outfitService, never()).getOutfitsByType(any(), any());
    }

    @Test
    void outfitsPage_withTypeParam_filtersByType() throws Exception {
        User user = authenticatedUser();
        List<Outfit> casualOutfits = List.of(new Outfit("Weekend look", user));
        when(outfitService.getOutfitsByType(any(User.class), eq("casual"))).thenReturn(casualOutfits);

        mockMvc.perform(get("/outfits").param("type", "casual"))
                .andExpect(status().isOk())
                .andExpect(view().name("outfit/index"))
                .andExpect(model().attribute("outfits", casualOutfits));

        verify(outfitService, never()).getOutfits(any());
    }

    @Test
    void newOutfitPage_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/outfits/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void newOutfitPage_returnsNewViewWithWardrobeWhenAuthenticated() throws Exception {
        User user = authenticatedUser();
        List<Clothes> wardrobe = List.of(new Clothes("Denim jacket", user));
        when(clothesService.getWardrobe(any(User.class))).thenReturn(wardrobe);

        mockMvc.perform(get("/outfits/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("outfit/new"))
                .andExpect(model().attribute("wardrobe", wardrobe));
    }

    @Test
    void createOutfit_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/outfits").param("outfitName", "Beach look"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(outfitService, never()).addOutfit(any(), any(), any(), any());
    }

    @Test
    void createOutfit_withoutAnImage_savesAndRedirectsToOutfits() throws Exception {
        authenticatedUser();

        mockMvc.perform(post("/outfits").param("outfitName", "Beach look"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/outfits"));

        verify(outfitService).addOutfit(any(User.class), eq("Beach look"), isNull(), isNull());
    }

    @Test
    void createOutfit_withAnImage_passesItThroughToTheService() throws Exception {
        authenticatedUser();
        MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", "bytes".getBytes());

        mockMvc.perform(multipart("/outfits")
                .file(image)
                .param("outfitName", "Beach look")
                .with(request -> {
                    request.setMethod("POST");
                    return request;
                }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/outfits"));

        verify(outfitService).addOutfit(any(User.class), eq("Beach look"), isNull(), eq((MultipartFile) image));
    }

    @Test
    void createOutfit_whenServiceRejectsTheUpload_returnsNewViewWithAnError() throws Exception {
        authenticatedUser();
        when(outfitService.addOutfit(any(), any(), any(), any()))
                .thenThrow(new IOException("Outfit photo must be an image"));

        mockMvc.perform(post("/outfits").param("outfitName", "Beach look"))
                .andExpect(status().isOk())
                .andExpect(view().name("outfit/new"))
                .andExpect(model().attributeExists("outfitError"));
    }

    @Test
    void editOutfitPage_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/outfits/1/edit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(outfitService, never()).getOutfitForEdit(any(), any());
    }

    @Test
    void editOutfitPage_returnsEditViewWithOutfitAndWardrobeWhenAuthenticated() throws Exception {
        User user = authenticatedUser();
        Outfit outfit = new Outfit("Beach look", user);
        List<Clothes> wardrobe = List.of(new Clothes("Denim jacket", user));
        when(outfitService.getOutfitForEdit(eq(1L), any(User.class))).thenReturn(outfit);
        when(clothesService.getWardrobe(any(User.class))).thenReturn(wardrobe);

        mockMvc.perform(get("/outfits/1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("outfit/edit"))
                .andExpect(model().attribute("outfit", outfit))
                .andExpect(model().attribute("wardrobe", wardrobe));
    }

    @Test
    void updateOutfit_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/outfits/1/edit").param("outfitName", "New look"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(outfitService, never()).updateOutfit(any(), any(), any(), any(), any());
    }

    @Test
    void updateOutfit_withoutAnImage_updatesAndRedirectsToOutfits() throws Exception {
        authenticatedUser();

        mockMvc.perform(post("/outfits/1/edit").param("outfitName", "New look"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/outfits"));

        verify(outfitService).updateOutfit(eq(1L), any(User.class), eq("New look"), isNull(), isNull());
    }

    @Test
    void updateOutfit_withAnImage_passesItThroughToTheService() throws Exception {
        authenticatedUser();
        MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", "bytes".getBytes());

        mockMvc.perform(multipart("/outfits/1/edit")
                .file(image)
                .param("outfitName", "New look")
                .with(request -> {
                    request.setMethod("POST");
                    return request;
                }))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/outfits"));

        verify(outfitService).updateOutfit(eq(1L), any(User.class), eq("New look"), isNull(), eq((MultipartFile) image));
    }

    @Test
    void updateOutfit_whenServiceRejectsTheUpload_returnsEditViewWithAnError() throws Exception {
        authenticatedUser();
        when(outfitService.updateOutfit(any(), any(), any(), any(), any()))
                .thenThrow(new IOException("Outfit photo must be an image"));

        mockMvc.perform(post("/outfits/1/edit").param("outfitName", "New look"))
                .andExpect(status().isOk())
                .andExpect(view().name("outfit/edit"))
                .andExpect(model().attributeExists("outfitError"));
    }

    @Test
    void deleteOutfit_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/outfits/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(outfitService, never()).deleteOutfit(any(), any());
    }

    @Test
    void deleteOutfit_deletesAndRedirectsToOutfitsWhenAuthenticated() throws Exception {
        authenticatedUser();

        mockMvc.perform(post("/outfits/1/delete"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/outfits"));

        verify(outfitService).deleteOutfit(eq(1L), any(User.class));
    }
}
