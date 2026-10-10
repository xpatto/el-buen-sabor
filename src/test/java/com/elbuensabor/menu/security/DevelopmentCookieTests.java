package com.elbuensabor.menu.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.elbuensabor.menu.SqliteIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("dev")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DevelopmentCookieTests extends SqliteIntegrationTest {

    @LocalServerPort
    int port;

    @Test
    void localHttpSessionCookieWorksWithoutSecureFlag() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/admin"))
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Set-Cookie").orElseThrow())
                    .contains("JSESSIONID=", "HttpOnly", "SameSite=Lax").doesNotContain("Secure");
        }
    }
}
