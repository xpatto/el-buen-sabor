package com.elbuensabor.menu.repository;

import com.elbuensabor.menu.SqliteIntegrationTest;
import com.elbuensabor.menu.domain.Categoria;
import com.elbuensabor.menu.domain.Producto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class MenuRepositoriesTests extends SqliteIntegrationTest {

    @Autowired
    CategoriaRepository categorias;

    @Autowired
    ProductoRepository productos;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void listsCategoriesWithTheirNamesAndSlugs() {
        assertThat(categorias.listarTodas()).containsExactly(
                new Categoria(1, "Comida / Lonchería", "comida"),
                new Categoria(2, "Cafetería y Bebidas", "bebidas"),
                new Categoria(3, "Tienda y Snacks", "tienda"));
    }

    @Test
    void listsDemoProductsWithExactIntegerPricesAndDescriptions() {
        assertThat(productos.listarActivos())
                .extracting(Producto::id, Producto::nombre, Producto::precioCentavos, Producto::categoriaId)
                .containsExactly(
                        tuple(1L, "Torta de jamón", 4500L, 1L),
                        tuple(2L, "Chilaquiles verdes", 7500L, 1L),
                        tuple(3L, "Café americano", 3000L, 2L),
                        tuple(4L, "Licuado de fresa", 4000L, 2L),
                        tuple(5L, "Refresco", 2500L, 2L),
                        tuple(6L, "Galletas", 1800L, 3L),
                        tuple(7L, "Papitas", 2200L, 3L));
        assertThat(productos.listarActivos()).allSatisfy(producto -> {
            assertThat(producto.descripcion()).isNotBlank();
            assertThat(producto.activo()).isTrue();
        });
        assertThat(productos.listarActivos().getFirst().descripcion())
                .isEqualTo("Jamón con queso y verduras.");
    }

    @ParameterizedTest
    @CsvSource({"comida, 1, 2", "bebidas, 2, 3", "tienda, 3, 2"})
    void filtersActiveProductsByCategorySlug(String slug, long categoryId, int count) {
        assertThat(productos.listarActivosPorCategoria(slug)).hasSize(count)
                .allSatisfy(producto -> {
                    assertThat(producto.categoriaId()).isEqualTo(categoryId);
                    assertThat(producto.activo()).isTrue();
                });
    }

    @Test
    void unknownOrSqlInjectionSlugReturnsNoProducts() {
        assertThat(productos.listarActivosPorCategoria("desconocida")).isEmpty();
        assertThat(productos.listarActivosPorCategoria("comida' OR 1=1 --")).isEmpty();
    }

    @Test
    @Transactional
    void excludesHiddenProductsFromBothReadOperations() {
        jdbc.update("UPDATE productos SET activo = ? WHERE id = ?", 0, 1);
        assertThat(productos.listarActivos()).hasSize(6)
                .extracting(Producto::id).doesNotContain(1L);
        assertThat(productos.listarActivosPorCategoria("comida"))
                .extracting(Producto::id).containsExactly(2L);
        jdbc.update("UPDATE productos SET activo = ? WHERE id = ?", 1, 1);
        assertThat(productos.listarActivosPorCategoria("comida"))
                .extracting(Producto::id).containsExactly(1L, 2L);
    }
}
