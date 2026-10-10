package com.elbuensabor.menu;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlitePersistenceTests extends SqliteIntegrationTest {

    @Autowired
    DataSource dataSource;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void connectsToSqliteFileIsolatedFromDevelopment() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("SQLite");
            assertThat(connection.getMetaData().getURL()).isEqualTo("jdbc:sqlite:" + TEST_DATABASE);
        }
        String file = jdbc.queryForObject("SELECT file FROM pragma_database_list WHERE name = 'main'", String.class);
        assertThat(Path.of(file).toAbsolutePath().normalize()).isEqualTo(TEST_DATABASE)
                .isNotEqualTo(Path.of("data", "menu.db").toAbsolutePath().normalize());
        assertThat(TEST_DATABASE).startsWith(Path.of("target").toAbsolutePath().normalize());
        assertThat(TEST_DATABASE).exists();
    }

    @Test
    void initializesThreeCategoriesAndSevenProducts() {
        assertThat(jdbc.queryForObject("SELECT count(*) FROM categorias", Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM productos", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForList("SELECT slug FROM categorias ORDER BY id", String.class))
                .containsExactly("comida", "bebidas", "tienda");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM productos WHERE activo = 1", Integer.class))
                .isEqualTo(7);
    }

    @Test
    void enablesForeignKeysOnEveryNewAndReusedPoolConnection() throws SQLException {
        HikariDataSource pool = (HikariDataSource) dataSource;
        for (int round = 0; round < 2; round++) {
            List<Connection> connections = new ArrayList<>();
            try {
                // Mantenerlas prestadas obliga a probar conexiones físicas diferentes.
                for (int index = 0; index < pool.getMaximumPoolSize(); index++) {
                    Connection connection = pool.getConnection();
                    connections.add(connection);
                    try (var statement = connection.createStatement();
                         var result = statement.executeQuery("PRAGMA foreign_keys")) {
                        assertThat(result.next()).isTrue();
                        assertThat(result.getInt(1)).isEqualTo(1);
                    }
                    try (var statement = connection.createStatement()) {
                        assertThatThrownBy(() -> statement.executeUpdate("""
                                INSERT INTO productos (nombre, descripcion, precio_centavos, categoria_id)
                                VALUES ('Inválido', 'Prueba', 100, -999)
                                """)).isInstanceOf(SQLException.class).hasMessageContaining("FOREIGN KEY");
                    }
                }
            } finally {
                for (Connection connection : connections) {
                    connection.close();
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "INSERT INTO categorias (nombre, slug) VALUES (NULL, 'otra')",
            "INSERT INTO categorias (nombre, slug) VALUES ('Otra', NULL)",
            "INSERT INTO categorias (nombre, slug) VALUES ('Otra', 'comida')",
            "INSERT INTO categorias (id, nombre, slug) VALUES (1, 'Otra', 'otra')",
            "UPDATE productos SET nombre = NULL WHERE id = 1",
            "UPDATE productos SET descripcion = NULL WHERE id = 1",
            "UPDATE productos SET precio_centavos = NULL WHERE id = 1",
            "UPDATE productos SET precio_centavos = 0 WHERE id = 1",
            "UPDATE productos SET precio_centavos = -1 WHERE id = 1",
            "UPDATE productos SET precio_centavos = 1.5 WHERE id = 1",
            "UPDATE productos SET precio_centavos = 'texto' WHERE id = 1",
            "UPDATE productos SET categoria_id = NULL WHERE id = 1",
            "UPDATE productos SET categoria_id = -999 WHERE id = 1",
            "UPDATE productos SET activo = NULL WHERE id = 1",
            "UPDATE productos SET activo = -1 WHERE id = 1",
            "UPDATE productos SET activo = 2 WHERE id = 1",
            "UPDATE productos SET activo = 0.5 WHERE id = 1",
            "DELETE FROM categorias WHERE id = 1",
            "UPDATE productos SET id = 2 WHERE id = 1"
    })
    @Transactional
    void rejectsInvalidData(String sql) {
        assertThatThrownBy(() -> jdbc.update(sql)).isInstanceOf(DataAccessException.class)
                .hasMessageContaining("SQLITE_CONSTRAINT");
    }

    @Test
    @Transactional
    void defaultsNewProductsToActive() {
        jdbc.update("""
                INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id)
                VALUES (?, ?, ?, ?, ?)
                """, 100, "Nuevo", "Prueba", 1500, 1);
        assertThat(jdbc.queryForObject("SELECT activo FROM productos WHERE id = 100", Integer.class))
                .isEqualTo(1);
    }

    @Test
    @Transactional
    void reinitializationPreservesEditsAndAdditionalDataWithoutDuplicates() {
        jdbc.update("""
                UPDATE productos SET nombre = ?, descripcion = ?, precio_centavos = ?, categoria_id = ?, activo = ?
                WHERE id = ?
                """, "Torta editada", "Descripción editada", 5100, 2, 0, 1);
        jdbc.update("UPDATE categorias SET nombre = ?, slug = ? WHERE id = ?", "Comida editada", "loncheria", 1);
        jdbc.update("INSERT INTO categorias (id, nombre, slug) VALUES (?, ?, ?)", 100, "Especiales", "especiales");
        jdbc.update("""
                INSERT INTO productos (id, nombre, descripcion, precio_centavos, categoria_id, activo)
                VALUES (?, ?, ?, ?, ?, ?)
                """, 100, "Especial", "Añadido posteriormente", 9900, 100, 1);
        var categoriesBefore = jdbc.queryForList("SELECT * FROM categorias ORDER BY id");
        var productsBefore = jdbc.queryForList("SELECT * FROM productos ORDER BY id");

        var scripts = new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql"), new ClassPathResource("data.sql"));
        scripts.setSqlScriptEncoding("UTF-8");
        scripts.execute(dataSource);
        scripts.execute(dataSource);

        assertThat(jdbc.queryForList("SELECT * FROM categorias ORDER BY id")).isEqualTo(categoriesBefore);
        assertThat(jdbc.queryForList("SELECT * FROM productos ORDER BY id")).isEqualTo(productsBefore);
    }
}
