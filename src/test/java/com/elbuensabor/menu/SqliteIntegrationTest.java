package com.elbuensabor.menu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.elbuensabor.menu.security.TestAdminCredentials;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
public abstract class SqliteIntegrationTest {

    protected static final Path TEST_DATABASE = createDatabasePath();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        // Tiene prioridad sobre APP_DB_PATH y la configuración de desarrollo.
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEST_DATABASE);
        registry.add("ADMIN_USERNAME", () -> TestAdminCredentials.USERNAME);
        registry.add("ADMIN_PASSWORD_HASH", () -> TestAdminCredentials.HASH);
    }

    private static Path createDatabasePath() {
        try {
            Path root = Files.createDirectories(Path.of("target", "test-databases"));
            return Files.createTempDirectory(root, "sqlite-").resolve("menu-test.db")
                    .toAbsolutePath().normalize();
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear el directorio SQLite de pruebas", exception);
        }
    }
}
