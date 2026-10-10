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

@ActiveProfiles("prod")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "TRUSTED_PROXY_IP_REGEX=(?!)"})
class UntrustedProxyTests extends SqliteIntegrationTest {

    @LocalServerPort
    int port;

    @Test
    void forgedForwardingHeadersDoNotBypassHttpsRequirement() throws Exception {
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/admin"))
                            .header("Forwarded", "for=127.0.0.1;proto=https;host=untrusted.invalid")
                            .header("X-Forwarded-Proto", "https")
                            .header("X-Forwarded-For", "127.0.0.1")
                            .header("X-Forwarded-Host", "untrusted.invalid")
                            .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(302);
            assertThat(response.headers().firstValue("Location").orElseThrow())
                    .isEqualTo("https://127.0.0.1:443/admin");
            assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        }
    }
}
