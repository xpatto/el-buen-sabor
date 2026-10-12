package com.elbuensabor.menu.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DemoStartupTests {
    @TempDir Path directory;

    private void rejected(String message, String... arguments) throws Exception {
        Files.createDirectories(directory.resolve("data"));
        Path log = directory.resolve("rejected.log");
        Process process = DemoApplicationTests.launch(directory, log, arguments);
        try {
            assertThat(process.waitFor(Duration.ofSeconds(35).toMillis(), TimeUnit.MILLISECONDS)).isTrue();
            assertThat(process.exitValue()).isNotZero();
            assertThat(Files.readString(log)).contains(message).doesNotContain("Tomcat started on port");
            assertThat(Files.exists(directory.resolve("data/menu-demo.db"))).isFalse();
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.0.0.0", "192.0.2.10", "::", "127.0.0.2", "localhost", ""})
    void rejectsEffectiveBindOutsideExactIpv4Loopback(String address) throws Exception {
        rejected("Demo requiere server.address=127.0.0.1", "--spring.profiles.active=demo",
                "--server.address=" + address, "--server.port=0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"demo,prod", "prod,demo", "demo,dev"})
    void rejectsMixedProfiles(String profiles) throws Exception {
        rejected("Demo debe ser el único perfil", "--spring.profiles.active=" + profiles, "--server.port=0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"spring.datasource.url", "spring.datasource.hikari.jdbc-url"})
    void rejectsDatabaseOverrideBeforeOpeningIt(String property) throws Exception {
        rejected("Demo requiere su base independiente", "--spring.profiles.active=demo",
                "--" + property + "=jdbc:sqlite:" + directory.resolve("data/menu.db"), "--server.port=0");
        assertThat(Files.exists(directory.resolve("data/menu.db"))).isFalse();
    }

    @Test
    void rejectsIncludedProductionProfile() throws Exception {
        rejected("Demo debe ser el único perfil", "--spring.profiles.active=demo",
                "--spring.profiles.include=prod", "--server.port=0");
    }

    @Test
    void defaultProfileStillRequiresCredentials() throws Exception {
        rejected("Configuración administrativa ausente o inválida", "--server.port=0");
    }

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void normalProfilesStillRejectMissingCredentials(String profile) throws Exception {
        rejected("Configuración administrativa ausente o inválida", "--spring.profiles.active=" + profile,
                "--server.address=127.0.0.1", "--server.port=0");
    }
}
