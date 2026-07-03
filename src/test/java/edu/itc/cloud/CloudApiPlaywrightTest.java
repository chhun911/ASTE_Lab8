package edu.itc.cloud;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.APIResponse;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.RequestOptions;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * HTTP-level tests driven by Playwright's API client (no browser needed).
 *
 * <p>Exercises the schema/JSON, regex, contains and exception methods against
 * the running app and attaches the response body to Allure as evidence.</p>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Epic("Private Cloud Storage")
class CloudApiPlaywrightTest {

    @LocalServerPort
    private int port;

    private Playwright playwright;
    private APIRequestContext request;
    private String token;

    @BeforeAll
    void setUp() {
        playwright = Playwright.create();
        request = playwright.request().newContext(
                new APIRequest.NewContextOptions().setBaseURL("http://localhost:" + port));

        // Register once and reuse the token across the whole class.
        APIResponse reg = request.post("/api/auth/register", RequestOptions.create()
                .setHeader("Content-Type", "application/json")
                .setData(Map.of("email", "api.user@itc.edu", "password", "Secret123!")));
        assertThat(reg.status()).isEqualTo(201); // content equals on an HTTP status
        token = JsonParser.parseString(reg.text()).getAsJsonObject()
                .get("token").getAsString();
    }

    @AfterAll
    void tearDown() {
        if (request != null) {
            request.dispose();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    private RequestOptions authed() {
        return RequestOptions.create().setHeader("Authorization", "Bearer " + token);
    }

    // SCHEMA / JSON + REGEX + CONTENT EQUALS -------------------------------
    @Test
    @Feature("Schema and JSON")
    @DisplayName("GET /api/me returns the expected shape and values")
    void profileContract() {
        APIResponse res = request.get("/api/me", authed());
        assertThat(res.status()).isEqualTo(200);

        String text = res.text();
        Allure.addAttachment("GET /api/me", "application/json", text, ".json");

        JsonObject body = JsonParser.parseString(text).getAsJsonObject();

        // schema: every required field is present
        assertThat(body.has("email")).isTrue();
        assertThat(body.has("displayName")).isTrue();
        assertThat(body.has("quotaBytes")).isTrue();
        assertThat(body.has("usedBytes")).isTrue();
        assertThat(body.has("freeBytes")).isTrue();

        // regex: email format
        assertThat(body.get("email").getAsString()).matches("^[\\w.+-]+@[\\w.-]+$");

        // content equals: a new account has exactly 50 MB and 0 used
        assertThat(body.get("quotaBytes").getAsLong()).isEqualTo(52_428_800L);
        assertThat(body.get("usedBytes").getAsLong()).isEqualTo(0L);
    }

    // CONTAINS -------------------------------------------------------------
    @Test
    @Feature("Contains")
    @DisplayName("Folder listing contains a folder we created")
    void folderListingContainsNewFolder() {
        APIResponse created = request.post("/api/folders", authed()
                .setHeader("Content-Type", "application/json")
                .setData(Map.of("name", "Documents")));
        assertThat(created.status()).isEqualTo(201);

        APIResponse list = request.get("/api/folders", authed());
        assertThat(list.status()).isEqualTo(200);
        assertThat(list.text()).contains("Documents"); // contains on the raw body
    }

    // EXCEPTION / ERROR PATH (HTTP) ----------------------------------------
    @Test
    @Feature("Exception")
    @DisplayName("Unauthenticated request is rejected with 403")
    void unauthenticatedIsForbidden() {
        APIResponse res = request.get("/api/me"); // no Authorization header
        assertThat(res.status()).isEqualTo(403);
        assertThat(res.text().getBytes(StandardCharsets.UTF_8).length).isGreaterThan(0);
    }
}
