package com.elbuensabor.menu.controller;

import com.elbuensabor.menu.exception.ApiExceptionHandler;
import com.elbuensabor.menu.service.CategoriaService;
import com.elbuensabor.menu.service.ProductoService;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiExceptionHandlerTests {

    @ParameterizedTest
    @ValueSource(strings = {"/api/categorias", "/api/productos", "/api/admin/productos"})
    void unexpectedFailureReturnsControlled500WithoutInternalDetails(String endpoint) throws Exception {
        CategoriaService categories = mock(CategoriaService.class);
        ProductoService products = mock(ProductoService.class);
        var failure = new DataAccessResourceFailureException(
                "SELECT * FROM productos; jdbc:sqlite:/private/menu.db; password=secret");
        when(categories.listarTodas()).thenThrow(failure);
        when(products.listarActivos(null)).thenThrow(failure);
        when(products.listarTodos()).thenThrow(failure);
        var mvc = MockMvcBuilders.standaloneSetup(
                new CategoriaController(categories), new ProductoController(products), new AdminProductoController(products))
                .setControllerAdvice(new ApiExceptionHandler()).build();

        mvc.perform(get(endpoint))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("""
                        {"codigo":"ERROR_INTERNO","mensaje":"No fue posible procesar la solicitud."}
                        """, JsonCompareMode.STRICT));
    }
}
