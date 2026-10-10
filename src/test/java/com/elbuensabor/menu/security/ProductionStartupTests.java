package com.elbuensabor.menu.security;

import java.nio.file.Path;

import com.elbuensabor.menu.MenuApplication;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionStartupTests {

    @TempDir
    Path directory;

    @ParameterizedTest
    @CsvSource(value = {"'', ''", "admin-test, ''", "'', invalid-test-hash", "admin-test, invalid-test-hash"})
    void productionApplicationRefusesMissingOrInvalidCredentials(String username, String hash) {
        assertThatThrownBy(() -> {
            try (var context = new SpringApplicationBuilder(MenuApplication.class).run(
                    "--spring.profiles.active=prod",
                    "--server.address=127.0.0.1",
                    "--server.port=0",
                    "--APP_DB_PATH=" + directory.resolve("startup.db"),
                    "--ADMIN_USERNAME=" + username,
                    "--ADMIN_PASSWORD_HASH=" + hash)) {
                throw new AssertionError("El arranque debió rechazar las credenciales");
            }
        }).isInstanceOf(org.springframework.beans.BeansException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasStackTraceContaining("Configuración administrativa ausente o inválida");
    }
}
