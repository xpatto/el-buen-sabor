package com.elbuensabor.menu;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteRestartTests {

    @TempDir
    Path directory;

    @Test
    void appDbPathIsUsedAndCommittedChangesSurviveContextRestart() {
        Path database = directory.resolve("restart.db");
        // La propiedad de prueba sustituye APP_DB_PATH sin cambiar la URL configurada.
        var application = new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(MenuApplication.class)
                .withPropertyValues("APP_DB_PATH=" + database);

        application.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            assertThat(context.getEnvironment().getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:sqlite:" + database);
            assertThat(jdbc.queryForObject(
                    "SELECT file FROM pragma_database_list WHERE name = 'main'", String.class))
                    .isEqualTo(database.toString());
            jdbc.update("UPDATE productos SET precio_centavos = ?, activo = ? WHERE id = ?", 5600, 0, 1);
        });

        application.run(context -> {
            assertThat(context).hasNotFailed();
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM categorias", Integer.class)).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM productos", Integer.class)).isEqualTo(7);
            assertThat(jdbc.queryForObject("SELECT precio_centavos FROM productos WHERE id = 1", Long.class))
                    .isEqualTo(5600L);
            assertThat(jdbc.queryForObject("SELECT activo FROM productos WHERE id = 1", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("PRAGMA foreign_keys", Integer.class)).isEqualTo(1);
        });
    }
}
