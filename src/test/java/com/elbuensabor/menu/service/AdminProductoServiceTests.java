package com.elbuensabor.menu.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import com.elbuensabor.menu.domain.Categoria;
import com.elbuensabor.menu.domain.Producto;
import com.elbuensabor.menu.dto.ProductoRequest;
import com.elbuensabor.menu.exception.ProductoInvalidoException;
import com.elbuensabor.menu.exception.ProductoNoEncontradoException;
import com.elbuensabor.menu.repository.CategoriaRepository;
import com.elbuensabor.menu.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminProductoServiceTests {

    private final ProductoRepository products = mock(ProductoRepository.class);
    private final CategoriaRepository categories = mock(CategoriaRepository.class);
    private final ProductoService service = new ProductoService(products, categories);

    @ParameterizedTest
    @CsvSource({"0.01,1", "12.34,1234", "92233720368547758.07,9223372036854775807", "1E2,10000"})
    void convertsPriceExactlyBeforeInsert(String price, long cents) {
        when(categories.listarTodas()).thenReturn(List.of(new Categoria(1, "Comida", "comida")));
        when(products.crear("N", "D", cents, 1)).thenReturn(new Producto(8, "N", "D", cents, 1, true));
        var result = service.crear(new ProductoRequest("N", "D", new BigDecimal(price), 1L));
        assertThat(result.precio()).isEqualByComparingTo(price);
        assertThat(result.activo()).isTrue();
        verify(products).crear("N", "D", cents, 1);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"0", "-0.01", "1.001", "1.000", "92233720368547758.08", "1E100", "1E-100"})
    void rejectsBadPricesBeforeAnyDatabaseOperation(String price) {
        var request = new ProductoRequest("N", "D", price == null ? null : new BigDecimal(price), 1L);
        assertThatThrownBy(() -> service.crear(request)).isInstanceOf(ProductoInvalidoException.class);
        assertThatThrownBy(() -> service.actualizar(1, request)).isInstanceOf(ProductoInvalidoException.class);
        verifyNoInteractions(products, categories);
    }

    @Test
    void unknownCategoryDoesNotWrite() {
        when(categories.listarTodas()).thenReturn(List.of(new Categoria(1, "Comida", "comida")));
        var request = new ProductoRequest("N", "D", BigDecimal.ONE, 2L);
        assertThatThrownBy(() -> service.crear(request)).isInstanceOf(ProductoInvalidoException.class);
        assertThatThrownBy(() -> service.actualizar(1, request)).isInstanceOf(ProductoInvalidoException.class);
        verifyNoInteractions(products);
    }

    @Test
    void updateReturnsActualStoredStateWithoutChangingIt() {
        when(categories.listarTodas()).thenReturn(List.of(new Categoria(2, "Bebidas", "bebidas")));
        when(products.actualizar(9, "Nuevo", "Otra", 1001, 2)).thenReturn(1);
        when(products.buscarPorId(9)).thenReturn(Optional.of(new Producto(9, "Nuevo", "Otra", 1001, 2, false)));
        var result = service.actualizar(9, new ProductoRequest("Nuevo", "Otra", new BigDecimal("10.01"), 2L));
        assertThat(result.activo()).isFalse();
        assertThat(result.categoria()).isEqualTo("Bebidas");
        verify(products, never()).cambiarEstado(anyLong(), anyBoolean());
    }

    @Test
    void missingProductsProduceNotFoundForBothUpdates() {
        when(categories.listarTodas()).thenReturn(List.of(new Categoria(1, "Comida", "comida")));
        assertThatThrownBy(() -> service.actualizar(99, new ProductoRequest("N", "D", BigDecimal.ONE, 1L)))
                .isInstanceOf(ProductoNoEncontradoException.class);
        assertThatThrownBy(() -> service.cambiarEstado(99, false)).isInstanceOf(ProductoNoEncontradoException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, Long.MIN_VALUE})
    void invalidIdsNeverReachRepository(long id) {
        assertThatThrownBy(() -> service.actualizar(id, new ProductoRequest("N", "D", BigDecimal.ONE, 1L)))
                .isInstanceOf(ProductoInvalidoException.class);
        assertThatThrownBy(() -> service.cambiarEstado(id, true)).isInstanceOf(ProductoInvalidoException.class);
        verifyNoInteractions(products, categories);
    }
}
