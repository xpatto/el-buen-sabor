package com.elbuensabor.menu.service;

import java.math.BigDecimal;
import java.util.List;

import com.elbuensabor.menu.domain.Categoria;
import com.elbuensabor.menu.domain.Producto;
import com.elbuensabor.menu.dto.ProductoResponse;
import com.elbuensabor.menu.exception.CategoriaInvalidaException;
import com.elbuensabor.menu.repository.CategoriaRepository;
import com.elbuensabor.menu.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProductoServiceTests {

    private final ProductoRepository products = mock(ProductoRepository.class);
    private final CategoriaRepository categories = mock(CategoriaRepository.class);
    private final ProductoService service = new ProductoService(products, categories);

    @ParameterizedTest
    @CsvSource({"4500, 45.00", "4501, 45.01", "1, 0.01", "9223372036854775807, 92233720368547758.07"})
    void convertsCentavosExactlyAndUsesReadableCategory(long centavos, String expected) {
        when(categories.listarTodas()).thenReturn(List.of(new Categoria(1, "Comida / Lonchería", "comida")));
        when(products.listarActivos()).thenReturn(List.of(new Producto(7, "Torta", "Con queso", centavos, 1, true)));

        assertThat(service.listarActivos(null)).containsExactly(
                new ProductoResponse(7, "Torta", "Comida / Lonchería", new BigDecimal(expected), "Con queso"));
    }

    @Test
    void validatesCategoryAgainstRepositoryRatherThanHardcodedSlugs() {
        when(categories.listarTodas()).thenReturn(List.of(new Categoria(80, "Categoría editada", "especial")));
        when(products.listarActivosPorCategoria("especial")).thenReturn(
                List.of(new Producto(9, "Especial", "Descripción", 1099, 80, true)));

        assertThat(service.listarActivos("especial")).containsExactly(
                new ProductoResponse(9, "Especial", "Categoría editada", new BigDecimal("10.99"), "Descripción"));
    }

    @Test
    void existingCategoryWithoutActiveProductsReturnsEmptyList() {
        when(categories.listarTodas()).thenReturn(List.of(new Categoria(1, "Comida", "comida")));
        when(products.listarActivosPorCategoria("comida")).thenReturn(List.of());
        assertThat(service.listarActivos("comida")).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "desconocida", "COMIDA", " comida ", "comida' OR 1=1 --"})
    void rejectsEmptyOrUnknownCategory(String slug) {
        when(categories.listarTodas()).thenReturn(List.of(new Categoria(1, "Comida", "comida")));
        assertThatThrownBy(() -> service.listarActivos(slug)).isInstanceOf(CategoriaInvalidaException.class);
        verifyNoInteractions(products);
    }
}
