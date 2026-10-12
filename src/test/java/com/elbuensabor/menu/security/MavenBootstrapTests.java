package com.elbuensabor.menu.security;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

class MavenBootstrapTests {
    @TempDir Path directory;

    @Test
    void refusesModifiedWrapperWithoutExecutingOrOverwritingIt() throws Exception {
        Path wrapper = directory.resolve(".mvn/wrapper/maven-wrapper.jar");
        Files.createDirectories(wrapper.getParent());
        Files.writeString(wrapper, "No es el wrapper oficial");
        Path log = directory.resolve("bootstrap.log");
        var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                Path.of("scripts/PrepararMaven.java").toAbsolutePath().toString(), directory.toString())
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try {
            assertThat(process.waitFor(20, TimeUnit.SECONDS)).isTrue();
            assertThat(process.exitValue()).isNotZero();
            assertThat(Files.readString(log)).contains("integridad del Maven Wrapper");
            assertThat(Files.readString(wrapper)).isEqualTo("No es el wrapper oficial");
        } finally {
            if (process.isAlive()) {
                process.destroyForcibly();
                process.waitFor(10, TimeUnit.SECONDS);
            }
        }
    }
}
