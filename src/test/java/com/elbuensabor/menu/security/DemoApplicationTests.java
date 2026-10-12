package com.elbuensabor.menu.security;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/** Tomcat real en otra JVM: directorio y SQLite propios, sin credenciales heredadas. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class DemoApplicationTests {
    @TempDir static Path directory;
    private Process server;
    private String base;
    private byte[] normalDatabase;
    private final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    void startDemoWithoutAdministrativeVariables() throws Exception {
        Files.createDirectories(directory.resolve("data"));
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + directory.resolve("data/menu.db"));
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE testigo (valor TEXT)");
            statement.execute("INSERT INTO testigo VALUES ('No modificar')");
        }
        normalDatabase = Files.readAllBytes(directory.resolve("data/menu.db"));
        start();
    }

    private void start() throws Exception {
        Path log = directory.resolve("startup-" + System.nanoTime() + ".log");
        server = launch(directory, log, "--spring.profiles.active=demo", "--server.port=0");
        long deadline = System.nanoTime() + Duration.ofSeconds(45).toNanos();
        while (System.nanoTime() < deadline && server.isAlive()) {
            var matcher = Pattern.compile("Tomcat started on port ([0-9]+)").matcher(Files.readString(log));
            if (matcher.find()) {
                base = "http://127.0.0.1:" + matcher.group(1);
                return;
            }
            Thread.sleep(100);
        }
        stop();
        throw new AssertionError("Demo no arrancó: " + Files.readString(log));
    }

    static Process launch(Path directory, Path log, String... arguments) throws Exception {
        String classpath = Arrays.stream(System.getProperty("java.class.path").split(java.io.File.pathSeparator))
                .map(entry -> Path.of(entry).toAbsolutePath().toString())
                .collect(java.util.stream.Collectors.joining(java.io.File.pathSeparator));
        var command = new ArrayList<>(java.util.List.of(
                Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-cp", classpath,
                "com.elbuensabor.menu.MenuApplication"));
        command.addAll(java.util.List.of(arguments));
        var builder = new ProcessBuilder(command).directory(directory.toFile())
                .redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().keySet().removeIf(key -> key.startsWith("ADMIN_") || key.startsWith("SPRING_")
                || key.startsWith("SERVER_") || key.equals("APP_DB_PATH") || key.equals("JAVA_TOOL_OPTIONS")
                || key.equals("JDK_JAVA_OPTIONS") || key.equals("_JAVA_OPTIONS"));
        // Un APP_DB_PATH heredado no debe redirigir el catálogo demo al normal.
        builder.environment().put("APP_DB_PATH", directory.resolve("data/menu.db").toString());
        return builder.start();
    }

    @AfterAll
    void stop() throws Exception {
        if (server != null && server.isAlive()) {
            server.destroy();
            if (!server.waitFor(15, TimeUnit.SECONDS)) {
                server.destroyForcibly();
                assertThat(server.waitFor(10, TimeUnit.SECONDS)).isTrue();
            }
        }
    }

    private HttpClient client() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
                .connectTimeout(Duration.ofSeconds(5)).build();
    }

    private HttpResponse<String> request(HttpClient client, String method, String path, String body, String token) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(10));
        if (token != null) builder.header("X-CSRF-TOKEN", token);
        if (body != null) builder.header("Content-Type", path.equals("/admin/login")
                ? "application/x-www-form-urlencoded" : "application/json");
        return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String token(String html, String regex) {
        var matcher = Pattern.compile(regex).matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private HttpResponse<String> login(HttpClient client, String password) throws Exception {
        var page = request(client, "GET", "/admin", null, null);
        assertThat(page.statusCode()).isEqualTo(200);
        String csrf = token(page.body(), "name=\"_csrf\" value=\"([^\"]+)\"");
        return request(client, "POST", "/admin/login", "username=admin&password=" + password + "&_csrf="
                + URLEncoder.encode(csrf, StandardCharsets.UTF_8), null);
    }

    private String authenticate(HttpClient client) throws Exception {
        var response = login(client, "admin");
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location").orElseThrow()).endsWith("/admin");
        var panel = request(client, "GET", "/admin", null, null);
        assertThat(panel.body()).contains("Tu catálogo").doesNotContain("name=\"password\"");
        return token(panel.body(), "name=\"csrf-token\" content=\"([^\"]+)\"");
    }

    @Test @Order(1)
    void newDemoHasPublicSeedsVariationsAndNoAdministrativeLink() throws Exception {
        try (var client = client()) {
            var page = request(client, "GET", "/", null, null);
            assertThat(page.statusCode()).isEqualTo(200);
            assertThat(page.body()).doesNotContain("/admin");
            assertThat(json.readTree(request(client, "GET", "/api/categorias", null, null).body())).hasSize(3);
            JsonNode products = json.readTree(request(client, "GET", "/api/productos", null, null).body());
            assertThat(products).hasSize(7);
            assertThat(products.get(1).get("descripcion").asText()).endsWith("Disponibles con o sin picante.");
            assertThat(products.get(3).get("descripcion").asText()).endsWith("Opción con leche regular o leche sin lactosa.");
            assertThat(products.get(1).get("precio").decimalValue()).isEqualByComparingTo("75.00");
            assertThat(products.get(3).get("precio").decimalValue()).isEqualByComparingTo("40.00");
            assertThat(json.readTree(request(client, "GET", "/api/productos?categoria=comida", null, null).body()))
                    .anySatisfy(product -> assertThat(product.get("id").asInt()).isEqualTo(2));
            assertThat(json.readTree(request(client, "GET", "/api/productos?categoria=bebidas", null, null).body()))
                    .anySatisfy(product -> assertThat(product.get("id").asInt()).isEqualTo(4));
        }
    }

    @Test @Order(2)
    void demoLoginUsesRealCsrfAndSession() throws Exception {
        try (var client = client()) {
            var page = request(client, "GET", "/admin", null, null);
            assertThat(page.headers().firstValue("Set-Cookie").orElseThrow())
                    .contains("HttpOnly", "SameSite=Lax").doesNotContain("Secure");
            authenticate(client);
            assertThat(request(client, "GET", "/api/admin/productos", null, null).statusCode()).isEqualTo(200);
        }
    }

    @Test @Order(3)
    void wrongPasswordAndAnonymousAdministrationAreRejected() throws Exception {
        try (var client = client()) {
            assertThat(login(client, "incorrecta").headers().firstValue("Location").orElseThrow()).endsWith("/admin?error");
            assertThat(request(client, "GET", "/api/admin/productos", null, null).statusCode()).isEqualTo(401);
        }
    }

    @Test @Order(4)
    void loginAndAdministrativeWritesRequireCsrf() throws Exception {
        try (var client = client()) {
            assertThat(request(client, "POST", "/admin/login", "username=admin&password=admin", null).statusCode()).isEqualTo(403);
            authenticate(client);
            assertThat(request(client, "DELETE", "/api/admin/productos/1", null, null).statusCode()).isEqualTo(403);
        }
    }

    @Test @Order(5)
    void authenticatedAdministrativeLifecycleAndLogoutWork() throws Exception {
        try (var client = client()) {
            String csrf = authenticate(client);
            String product = "{\"nombre\":\"Prueba demo\",\"descripcion\":\"Temporal\",\"precio\":12.50,\"categoriaId\":1}";
            var created = request(client, "POST", "/api/admin/productos", product, csrf);
            assertThat(created.statusCode()).isEqualTo(201);
            String path = "/api/admin/productos/" + json.readTree(created.body()).get("id").asLong();
            assertThat(request(client, "PUT", path, product.replace("Temporal", "Editado"), csrf).statusCode()).isEqualTo(200);
            for (boolean active : new boolean[]{false, true}) {
                var changed = request(client, "PATCH", path + "/estado", "{\"activo\":" + active + "}", csrf);
                assertThat(changed.statusCode()).isEqualTo(200);
                assertThat(json.readTree(changed.body()).get("activo").asBoolean()).isEqualTo(active);
            }
            assertThat(request(client, "DELETE", path, null, csrf).statusCode()).isEqualTo(204);
            assertThat(request(client, "POST", "/admin/logout", null, csrf).statusCode()).isEqualTo(302);
            assertThat(request(client, "GET", "/api/admin/productos", null, null).statusCode()).isEqualTo(401);
        }
    }

    @Test @Order(6)
    void restartPreservesEditsAndDeletionsWithoutTouchingNormalDatabase() throws Exception {
        try (var client = client()) {
            String csrf = authenticate(client);
            assertThat(request(client, "PUT", "/api/admin/productos/2",
                    "{\"nombre\":\"Chilaquiles verdes\",\"descripcion\":\"Edición demo persistente\",\"precio\":75,\"categoriaId\":1}", csrf)
                    .statusCode()).isEqualTo(200);
            assertThat(request(client, "DELETE", "/api/admin/productos/7", null, csrf).statusCode()).isEqualTo(204);
        }
        stop();
        start();
        try (var client = client()) {
            JsonNode products = json.readTree(request(client, "GET", "/api/productos", null, null).body());
            assertThat(products).hasSize(6).noneSatisfy(p -> assertThat(p.get("id").asInt()).isEqualTo(7));
            assertThat(products.get(1).get("descripcion").asText()).isEqualTo("Edición demo persistente");
        }
        assertThat(Files.exists(directory.resolve("data/menu-demo.db"))).isTrue();
        assertThat(Files.readAllBytes(directory.resolve("data/menu.db"))).isEqualTo(normalDatabase);
    }
}
