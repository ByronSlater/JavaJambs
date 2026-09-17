package com.javajambs.cher.accessibility;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.deque.html.axecore.results.AxeResults;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessibilityTest {

    private static final String BASE_URL = "http://localhost:8080";

    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeEach
    void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true)
        );
        page = browser.newPage();
    }

    @AfterEach
    void tearDown() {
        browser.close();
        playwright.close();
    }

    @Test
    void dashboardShouldHaveNoAccessibilityViolations() {
        assertPageHasNoAccessibilityViolations("/dashboard");
    }

    @Test
    void homePageShouldHaveNoAccessibilityViolations() {
        assertPageHasNoAccessibilityViolations("/home");
    }

    @Test
    void jimpPageShouldHaveNoAccessibilityViolations() {
        assertPageHasNoAccessibilityViolations("/jimp");
    }

    @Test
    void loginPageShouldHaveNoAccessibilityViolations() {
        assertPageHasNoAccessibilityViolations("/login");
    }

    @Test
    void registerPageShouldHaveNoAccessibilityViolations() {
        assertPageHasNoAccessibilityViolations("/register");
    }

    @Test
    void profilePageShouldHaveNoAccessibilityViolations() {
        registerAndLogIn();
        page.navigate(BASE_URL + "/profile");

        assertNoAccessibilityViolations(page);
    }

    @Test
    void editProfilePageShouldHaveNoAccessibilityViolations() {
        registerAndLogIn();
        page.navigate(BASE_URL + "/profile/edit");

        assertNoAccessibilityViolations(page);
    }

    @Test
    void postsPageShouldHaveNoAccessibilityViolations() {
        registerAndLogIn();
        page.navigate(BASE_URL + "/posts");

        assertNoAccessibilityViolations(page);
    }

    private void assertPageHasNoAccessibilityViolations(String path) {
        page.navigate(BASE_URL + path);

        assertNoAccessibilityViolations(page);
    }

    private void assertNoAccessibilityViolations(Page page) {
        AxeResults results = new AxeBuilder(page).analyze();

        assertTrue(
                results.getViolations().isEmpty(),
                "Accessibility violations found: " + results.getViolations()
        );
    }

    /**
     * Registers a fresh user and logs in through the real forms so /profile
     * and /profile/edit render their authenticated content. Forms are
     * hx-boosted, so submission swaps content via AJAX rather than a plain
     * POST/redirect - we wait for the URL to move off the form page instead
     * of assuming a specific full-page navigation.
     */
    private void registerAndLogIn() {
        String username = "a11y-user-" + System.currentTimeMillis();
        String password = "AccessibilityTest123";

        page.navigate(BASE_URL + "/register");
        page.fill("#username", username);
        page.fill("#email", username + "@example.com");
        page.fill("#password", password);
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Create account")).click();
        page.waitForURL(url -> !url.contains("/register"));

        page.navigate(BASE_URL + "/login");
        page.fill("#username", username);
        page.fill("#password", password);
        page.getByRole(com.microsoft.playwright.options.AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName("Log in")).click();
        page.waitForURL(url -> !url.contains("/login"));
    }
}
