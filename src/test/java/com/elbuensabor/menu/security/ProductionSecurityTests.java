package com.elbuensabor.menu.security;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.elbuensabor.menu.SqliteIntegrationTest;
import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("prod")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"server.address=127.0.0.1", "TRUSTED_PROXY_IP_REGEX=127[.]0[.]0[.]1"})
class ProductionSecurityTests extends SqliteIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    ServletContext servletContext;

    @Test
    void trustedHttpsProxyGetsSecureSessionCookieAndHsts() throws Exception {
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/admin"))
                    .header("X-Forwarded-Proto", "https").GET().build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers().firstValue("Set-Cookie").orElseThrow())
                    .contains("JSESSIONID=", "Secure", "HttpOnly", "SameSite=Lax");
            assertThat(response.headers().firstValue("Strict-Transport-Security").orElseThrow())
                    .contains("max-age=31536000");
            assertThat(response.headers().firstValue("Cache-Control").orElseThrow()).contains("no-store");
            assertThat(servletContext.getSessionTimeout()).isEqualTo(30);
            assertThat(servletContext.getEffectiveSessionTrackingModes()).containsExactly(jakarta.servlet.SessionTrackingMode.COOKIE);
        }
    }

    @Test
    void productionHttpRedirectsToHttps() throws Exception {
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/admin")).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(302);
            assertThat(response.headers().firstValue("Location").orElseThrow()).isEqualTo("https://127.0.0.1:443/admin");
            assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        }
    }
    @Test
    void renderedCsrfAllowsRealLoginAndRotatesSecureSession() throws Exception {
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()) {
            var page = client.send(request("/admin").GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(page.statusCode()).isEqualTo(200);
            String cookie = page.headers().firstValue("Set-Cookie").orElseThrow().split(";")[0];
            var token = java.util.regex.Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
                    .matcher(page.body());
            assertThat(token.find()).isTrue();
            String body = "_csrf=" + java.net.URLEncoder.encode(token.group(1), java.nio.charset.StandardCharsets.UTF_8)
                    + "&username=" + TestAdminCredentials.USERNAME + "&password=" + TestAdminCredentials.PASSWORD;
            var login = client.send(request("/admin/login").header("Cookie", cookie)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            assertThat(login.statusCode()).isEqualTo(302);
            assertThat(login.headers().firstValue("Location").orElseThrow()).endsWith("/admin");
            String sessionCookie = login.headers().firstValue("Set-Cookie").orElseThrow();
            assertThat(sessionCookie).contains("Secure", "HttpOnly", "SameSite=Lax");
            String authenticatedCookie = sessionCookie.split(";")[0];
            assertThat(authenticatedCookie).isNotEqualTo(cookie);
            var admin = client.send(request("/admin").header("Cookie", authenticatedCookie).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(admin.statusCode()).isEqualTo(200);
            assertThat(admin.body()).contains("Administración provisional").doesNotContain("name=\"password\"");
            var anonymous = client.send(request("/api/admin/productos").GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(anonymous.statusCode()).isEqualTo(401);
            var authorized = client.send(request("/api/admin/productos").header("Cookie", authenticatedCookie).GET().build(),
                    HttpResponse.BodyHandlers.ofString());
            assertThat(authorized.statusCode()).isEqualTo(200);
            for (String path : new String[]{"/api/categorias", "/api/productos", "/api/productos?categoria=comida"}) {
                var response = client.send(request(path).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.body()).startsWith("[");
            }
        }
    }

    @Test
    void trustedProxyHttpRedirectUsesPublic443AndIgnoresForwardedHostAndPort() throws Exception {
        try (var client = HttpClient.newHttpClient()) {
            var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/admin?error"))
                    .header("X-Forwarded-Proto", "http")
                    .header("X-Forwarded-Host", "untrusted.invalid")
                    .header("X-Forwarded-Port", "8443")
                    .header("Forwarded", "proto=https;host=untrusted.invalid")
                    .GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(response.statusCode()).isEqualTo(302);
            URI location = URI.create(response.headers().firstValue("Location").orElseThrow());
            assertThat(location.getScheme()).isEqualTo("https");
            assertThat(location.getHost()).isEqualTo("127.0.0.1");
            assertThat(location.getPort() == -1 ? 443 : location.getPort()).isEqualTo(443);
            assertThat(location.getPath()).isEqualTo("/admin");
            assertThat(location.getQuery()).isEqualTo("error");
            assertThat(response.headers().allValues("Set-Cookie")).isEmpty();
        }
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("X-Forwarded-Proto", "https");
    }

}
