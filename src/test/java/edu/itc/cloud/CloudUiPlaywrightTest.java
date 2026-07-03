package edu.itc.cloud;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.io.ByteArrayInputStream;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Browser-driven UI test: registers through the Thymeleaf UI, then verifies the
 * dashboard with Playwright's auto-waiting web assertions (content equals,
 * contains, regex) and captures a screenshot as a visual snapshot in Allure.
 *
 * <p>On first run Playwright downloads a Chromium browser, so the first build
 * needs internet access. The service and API tests do not need a browser.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("Private Cloud Storage")
class CloudUiPlaywrightTest {

    @LocalServerPort
    private int port;

    private Playwright playwright;
    private Browser browser;
    private Page page;

    @BeforeAll
    void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(
                new BrowserType.LaunchOptions().setHeadless(true));
        page = browser.newPage();
    }

    @AfterAll
    void tearDown() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @Test
    @Feature("Visual and snapshot")
    @DisplayName("Register via UI and see the 50 MB quota on the dashboard")
    void registerThroughUiAndSeeQuota() {
        page.navigate("http://localhost:" + port + "/login");

        Allure.step("Fill the register form and submit", () -> {
            page.locator("#tab-register").click(); // reveal the register form
            page.getByTestId("register-email").fill("ui.user@itc.edu");
            page.getByTestId("register-password").fill("Secret123!");
            page.getByTestId("register-submit").click();
        });

        // regex: we land on the dashboard
        assertThat(page).hasURL(Pattern.compile(".*/dashboard$"));
        // contains: heading text is present
        assertThat(page.getByRole(AriaRole.HEADING,
                new Page.GetByRoleOptions().setName("My Cloud Storage"))).isVisible();
        // content equals: quota shows exactly "50 MB"
        assertThat(page.getByTestId("quota")).hasText("50 MB");

        // visual/snapshot evidence attached to the Allure report
        Allure.addAttachment("dashboard", "image/png",
                new ByteArrayInputStream(page.screenshot()), ".png");
    }
}
