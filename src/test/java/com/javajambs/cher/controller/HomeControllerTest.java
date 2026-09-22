package com.javajambs.cher.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.test.context.TestSecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;

import com.javajambs.cher.clothes.Clothes;
import com.javajambs.cher.clothes.ClothesService;
import com.javajambs.cher.user.User;

/**
 * Thymeleaf is excluded: rendering "carousel" for real would need the full
 * layout-dialect stack. A ViewResolver stands in for it, same as
 * ClothesControllerTest does for "clothes/*".
 */
@WebMvcTest(controllers = HomeController.class, excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(HomeControllerTest.NoOpViewResolverConfig.class)
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

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
    void carousel_redirectsToLoginWhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/carousel"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verifyNoInteractions(clothesService);
    }

    @Test
    void carousel_groupsWardrobeByTypeWhenAuthenticated() throws Exception {
        User user = authenticatedUser();
        Clothes hat = new Clothes("Beanie", user);
        Clothes top = new Clothes("Tee", user);
        Clothes pant = new Clothes("Jeans", user);
        Clothes shoe = new Clothes("Sneakers", user);

        when(clothesService.getWardrobeByType(any(User.class), eq("hat"))).thenReturn(List.of(hat));
        when(clothesService.getWardrobeByType(any(User.class), eq("top"))).thenReturn(List.of(top));
        when(clothesService.getWardrobeByType(any(User.class), eq("pant"))).thenReturn(List.of(pant));
        when(clothesService.getWardrobeByType(any(User.class), eq("shoe"))).thenReturn(List.of(shoe));

        mockMvc.perform(get("/carousel"))
                .andExpect(status().isOk())
                .andExpect(view().name("carousel"))
                .andExpect(model().attributeExists("closets"));

        verify(clothesService).getWardrobeByType(any(User.class), eq("hat"));
        verify(clothesService).getWardrobeByType(any(User.class), eq("top"));
        verify(clothesService).getWardrobeByType(any(User.class), eq("pant"));
        verify(clothesService).getWardrobeByType(any(User.class), eq("shoe"));
    }
}
