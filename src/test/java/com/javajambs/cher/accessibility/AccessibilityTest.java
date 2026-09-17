package com.javajambs.cher.accessibility;

import com.deque.html.axecore.playwright.AxeBuilder;
import com.deque.html.axecore.results.AxeResults;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AccessibilityTest {

    @Test
    void homepageShouldHaveNoAccessibilityViolations() {
        try (Playwright playwright = Playwright.create()) {

            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(true)
            );

            Page page = browser.newPage();

            page.navigate("http://localhost:8080");

            AxeResults results = new AxeBuilder(page).analyze();

            assertTrue(
                    results.getViolations().isEmpty(),
                    "Accessibility violations found: " + results.getViolations()
            );

            browser.close();
        }
    }
}