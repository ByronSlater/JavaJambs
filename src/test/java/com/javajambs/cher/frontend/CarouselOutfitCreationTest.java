package com.javajambs.cher.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;

/**
 * Real-browser, front-end-only integration tests for the carousel
 * (/carousel) and outfit-creation (/outfits) flow: drives the actual
 * rendered pages and carousel-picker.js the same way a user would, the same
 * pattern AccessibilityTest uses (no MockMvc, no mocked views). Assumes the
 * app is already running (e.g. via `make run`) against a real Postgres -
 * there is no self-booting @SpringBootTest here, matching AccessibilityTest.
 */
class CarouselOutfitCreationTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final Path HAT_IMAGE = Paths
            .get("src/main/resources/static/images/carousel/hats/red-snapback.png")
            .toAbsolutePath();

    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
        page = browser.newPage();
    }

    @AfterEach
    void tearDown() {
        browser.close();
        playwright.close();
    }

    @Test
    void carousel_redirectsToLoginWhenNotAuthenticated() {
        page.navigate(BASE_URL + "/carousel");

        assertTrue(page.url().contains("/login"));
    }

    @Test
    void carousel_showsEmptyStateWhenWardrobeIsEmpty() {
        registerAndLogIn("carousel-empty");

        page.navigate(BASE_URL + "/carousel");

        assertTrue(page.getByText("No Hats in your wardrobe yet").isVisible());
    }

    @Test
    void carousel_showsAnUploadedItemAndSelectsItAutomatically() {
        registerAndLogIn("carousel-single");
        addClothingItem("Test Hat", "hat", HAT_IMAGE);

        page.navigate(BASE_URL + "/carousel");

        Locator hatItems = page.locator("[data-carousel=\"hat\"] .carousel__item[data-clothes-id]");
        assertEquals(1, hatItems.count());

        String itemId = hatItems.first().getAttribute("data-clothes-id");
        String selectedId = page.locator("input[data-type=\"hat\"]").inputValue();
        assertEquals(itemId, selectedId);
    }

    @Test
    void scrollingCarouselRow_updatesTheSelectedItem() {
        registerAndLogIn("carousel-scroll");
        addClothingItem("Hat A", "hat", HAT_IMAGE);
        addClothingItem("Hat B", "hat", HAT_IMAGE);

        page.navigate(BASE_URL + "/carousel");

        Locator hiddenInput = page.locator("input[data-type=\"hat\"]");
        String before = hiddenInput.inputValue();
        assertFalse(before.isBlank());

        page.locator("[data-carousel=\"hat\"] .carousel__track")
                .evaluate("el => { el.scrollLeft += 300 }");
        page.waitForTimeout(600);

        String after = hiddenInput.inputValue();
        assertNotEquals(before, after, "Scrolling should move the tracked/selected item");
        assertFalse(after.isBlank());
    }

    @Test
    void savingAnOutfit_createsItFromTheCurrentlySelectedItems() {
        registerAndLogIn("outfit-save");
        addClothingItem("Save Test Hat", "hat", HAT_IMAGE);
        addClothingItem("Save Test Top", "top", HAT_IMAGE);
        addClothingItem("Save Test Pant", "pant", HAT_IMAGE);
        addClothingItem("Save Test Shoe", "shoe", HAT_IMAGE);

        page.navigate(BASE_URL + "/carousel");

        String outfitName = "E2E Outfit " + System.currentTimeMillis();
        page.fill("input[name=\"outfitName\"]", outfitName);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save Outfit")).click();
        page.waitForURL(url -> url.contains("/outfits"));

        assertTrue(page.getByText(outfitName).isVisible());
    }

    private void registerAndLogIn(String usernamePrefix) {
        String username = usernamePrefix + "-" + System.currentTimeMillis();
        String password = "CarouselTest123";

        page.navigate(BASE_URL + "/register");
        page.fill("#username", username);
        page.fill("#email", username + "@example.com");
        page.fill("#password", password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Create account")).click();
        page.waitForURL(url -> !url.contains("/register"));

        page.navigate(BASE_URL + "/login");
        page.fill("#username", username);
        page.fill("#password", password);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Log in")).click();
        page.waitForURL(url -> !url.contains("/login"));
    }

    private void addClothingItem(String name, String type, Path imagePath) {
        page.navigate(BASE_URL + "/clothes/new");
        page.fill("#name", name);
        page.selectOption("#type", type);
        page.setInputFiles("#image", imagePath);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Add to wardrobe")).click();
        page.waitForURL(url -> url.endsWith("/clothes"));
    }
}
