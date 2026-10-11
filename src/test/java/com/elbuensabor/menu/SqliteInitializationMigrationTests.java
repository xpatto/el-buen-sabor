package com.elbuensabor.menu;

import java.nio.file.Path;
import java.sql.DriverManager;

import com.elbuensabor.menu.security.TestAdminCredentials;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class SqliteInitializationMigrationTests {

    @TempDir Path directory;

    private String url() {
        return "jdbc:sqlite:" + directory.resolve("migration.db");
    }

    private ApplicationContextRunner application() {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(MenuApplication.class)
                .withPropertyValues("spring.datasource.url=" + url(),
                        "ADMIN_USERNAME=" + TestAdminCredentials.USERNAME,
                        "ADMIN_PASSWORD_HASH=" + TestAdminCredentials.HASH);
    }

    @Test
    void newDatabaseGetsSeedsOnceAndDeletedSeedNeverReturns() {
        application().run(context -> {
            assertThat(context).hasNotFailed();
            var jdbc = context.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM categorias", Integer.class)).isEqualTo(3);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM productos", Integer.class)).isEqualTo(7);
            context.getBean(com.elbuensabor.menu.service.ProductoService.class).eliminar(1);
            jdbc.update("UPDATE productos SET precio_centavos=1234, descripcion='Edición preservada' WHERE id=2");
            jdbc.update("INSERT INTO productos (id,nombre,descripcion,precio_centavos,categoria_id) VALUES (99,'Nuevo','Añadido',100,1)");
        });
        application().run(context -> {
            assertThat(context).hasNotFailed();
            var jdbc = context.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM productos WHERE id=1", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT precio_centavos FROM productos WHERE id=2", Long.class)).isEqualTo(1234L);
            assertThat(jdbc.queryForObject("SELECT descripcion FROM productos WHERE id=2", String.class)).isEqualTo("Edición preservada");
            assertThat(jdbc.queryForObject("SELECT count(*) FROM productos WHERE id=99", Integer.class)).isEqualTo(1);
        });
    }

    @Test
    void deletingAllProductsDoesNotRegenerateSeeds() {
        application().run(context -> {
            assertThat(context).hasNotFailed();
            var service = context.getBean(com.elbuensabor.menu.service.ProductoService.class);
            service.listarTodos().forEach(product -> service.eliminar(product.id()));
        });
        for (int restart = 0; restart < 2; restart++) {
            application().run(context -> {
                assertThat(context).hasNotFailed();
                var jdbc = context.getBean(JdbcTemplate.class);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM productos", Integer.class)).isZero();
                assertThat(jdbc.queryForObject("SELECT count(*) FROM categorias", Integer.class)).isEqualTo(3);
            });
        }
    }

    @Test
    void newProductNeverReusesDeletedHighestIdEvenAfterEmptyCatalogAndRestart() {
        application().run(context -> {
            assertThat(context).hasNotFailed();
            var service = context.getBean(com.elbuensabor.menu.service.ProductoService.class);
            service.listarTodos().forEach(product -> service.eliminar(product.id()));
        });
        application().run(context -> {
            assertThat(context).hasNotFailed();
            var service = context.getBean(com.elbuensabor.menu.service.ProductoService.class);
            var created = service.crear(new com.elbuensabor.menu.dto.ProductoRequest("Nuevo", "D", java.math.BigDecimal.ONE, 1L));
            assertThat(created.id()).isGreaterThan(7);
            service.eliminar(created.id());
        });
        application().run(context -> {
            assertThat(context).hasNotFailed();
            var service = context.getBean(com.elbuensabor.menu.service.ProductoService.class);
            var created = service.crear(new com.elbuensabor.menu.dto.ProductoRequest("Otro", "D", java.math.BigDecimal.ONE, 1L));
            assertThat(created.id()).isGreaterThan(8);
        });
    }

    // Esquema histórico explícito: la prueba no depende del schema.sql nuevo.
    private void createLegacyDatabase(boolean withRows) throws Exception {
        try (var connection = DriverManager.getConnection(url()); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE categorias (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL, slug TEXT NOT NULL UNIQUE)");
            statement.execute("""
                    CREATE TABLE productos (id INTEGER PRIMARY KEY, nombre TEXT NOT NULL, descripcion TEXT NOT NULL,
                    precio_centavos INTEGER NOT NULL CHECK(typeof(precio_centavos)='integer' AND precio_centavos>0),
                    categoria_id INTEGER NOT NULL REFERENCES categorias(id),
                    activo INTEGER NOT NULL DEFAULT 1 CHECK(activo IN (0,1)))
                    """);
            if (withRows) {
                statement.execute("INSERT INTO categorias VALUES (1,'Categoría editada','comida'),(2,'Bebidas','bebidas'),(3,'Tienda','tienda')");
                // Faltan productos iniciales; otros fueron editados y hay altas administrativas.
                statement.execute("INSERT INTO productos VALUES (2,'Editado','Descripción conservada',123456,2,0),(80,'Propio','Alta anterior',4567,1,1)");
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void legacySchemaIsPreservedEvenWithMissingSeedsOrEmptyTables(boolean withRows) throws Exception {
        createLegacyDatabase(withRows);
        for (int restart = 0; restart < 2; restart++) {
            application().run(context -> {
                assertThat(context).hasNotFailed();
                var jdbc = context.getBean(JdbcTemplate.class);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM productos", Integer.class)).isEqualTo(withRows ? 2 : 0);
                assertThat(jdbc.queryForObject("SELECT count(*) FROM categorias", Integer.class)).isEqualTo(withRows ? 3 : 0);
                if (withRows) {
                    assertThat(jdbc.queryForObject("SELECT nombre FROM categorias WHERE id=1", String.class)).isEqualTo("Categoría editada");
                    assertThat(jdbc.queryForObject("SELECT descripcion FROM productos WHERE id=2", String.class)).isEqualTo("Descripción conservada");
                    assertThat(jdbc.queryForObject("SELECT precio_centavos FROM productos WHERE id=2", Long.class)).isEqualTo(123456L);
                    assertThat(jdbc.queryForObject("SELECT activo FROM productos WHERE id=2", Integer.class)).isZero();
                    assertThat(jdbc.queryForObject("SELECT nombre FROM productos WHERE id=80", String.class)).isEqualTo("Propio");
                }
            });
        }
    }

    @Test
    void legacyDatabaseWithCategoriesButNoProductsStaysEmpty() throws Exception {
        createLegacyDatabase(true);
        try (var connection = DriverManager.getConnection(url()); var statement = connection.createStatement()) {
            statement.execute("DELETE FROM productos");
        }
        application().run(context -> {
            assertThat(context).hasNotFailed();
            var jdbc = context.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM productos", Integer.class)).isZero();
            assertThat(jdbc.queryForObject("SELECT count(*) FROM categorias", Integer.class)).isEqualTo(3);
        });
    }
    @Test
    void interruptedNewSchemaStillLoadsSeedsOnNextStartup() throws Exception {
        try (var connection = DriverManager.getConnection(url())) {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                    new org.springframework.core.io.ClassPathResource("schema.sql"));
        }
        application().run(context -> {
            assertThat(context).hasNotFailed();
            var jdbc = context.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("SELECT count(*) FROM productos", Integer.class)).isEqualTo(7);
            assertThat(jdbc.queryForObject("SELECT completada FROM inicializacion_catalogo WHERE id=1", Integer.class)).isEqualTo(1);
        });
    }

    @Test
    void failedSeedTransactionDoesNotCommitPartialCatalogOrCompletionFlag() throws Exception {
        try (var connection = DriverManager.getConnection(url()); var statement = connection.createStatement()) {
            org.springframework.jdbc.datasource.init.ScriptUtils.executeSqlScript(connection,
                    new org.springframework.core.io.ClassPathResource("schema.sql"));
            statement.execute("""
                    CREATE TRIGGER impedir_semillas BEFORE INSERT ON productos WHEN NEW.id=4
                    BEGIN SELECT RAISE(ABORT, 'Fallo de prueba'); END
                    """);
        }
        application().run(context -> assertThat(context).hasFailed());
        try (var connection = DriverManager.getConnection(url()); var statement = connection.createStatement()) {
            try (var result = statement.executeQuery("SELECT count(*) FROM productos")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
            try (var result = statement.executeQuery("SELECT completada FROM inicializacion_catalogo WHERE id=1")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
            try (var result = statement.executeQuery("SELECT count(*) FROM categorias")) {
                assertThat(result.next()).isTrue();
                assertThat(result.getInt(1)).isZero();
            }
            statement.execute("DROP TRIGGER impedir_semillas");
        }
        application().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(JdbcTemplate.class).queryForObject("SELECT count(*) FROM productos", Integer.class)).isEqualTo(7);
        });
    }

}
