package com.elbuensabor.menu.service;

import java.util.List;

import com.elbuensabor.menu.domain.Categoria;
import com.elbuensabor.menu.dto.CategoriaResponse;
import com.elbuensabor.menu.repository.CategoriaRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CategoriaServiceTests {

    private final CategoriaRepository repository = mock(CategoriaRepository.class);
    private final CategoriaService service = new CategoriaService(repository);

    @Test
    void mapsStoredCategoriesToResponseDtos() {
        when(repository.listarTodas()).thenReturn(List.of(
                new Categoria(1, "Comida / Lonchería", "comida"),
                new Categoria(2, "Cafetería y Bebidas", "bebidas"),
                new Categoria(3, "Tienda y Snacks", "tienda")));

        assertThat(service.listarTodas()).containsExactly(
                new CategoriaResponse(1, "Comida / Lonchería", "comida"),
                new CategoriaResponse(2, "Cafetería y Bebidas", "bebidas"),
                new CategoriaResponse(3, "Tienda y Snacks", "tienda"));
    }

    @Test
    void returnsEmptyListWhenThereAreNoCategories() {
        when(repository.listarTodas()).thenReturn(List.of());
        assertThat(service.listarTodas()).isEmpty();
    }
}
