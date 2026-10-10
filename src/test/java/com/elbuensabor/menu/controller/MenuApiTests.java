package com.elbuensabor.menu.controller;

import java.math.BigDecimal;

import com.elbuensabor.menu.SqliteIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MenuApiTests extends SqliteIntegrationTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void returnsThreeCategoriesWithPublicFieldsOnly() throws Exception {
        mvc.perform(get("/api/categorias"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        [
                          {"id":1,"nombre":"Comida / Lonchería","slug":"comida"},
                          {"id":2,"nombre":"Cafetería y Bebidas","slug":"bebidas"},
                          {"id":3,"nombre":"Tienda y Snacks","slug":"tienda"}
                        ]
                        """, org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test
    void returnsSevenActiveProductsWithPublicFieldsOnly() throws Exception {
        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Torta de jamón"))
                .andExpect(jsonPath("$[0].categoria").value("Comida / Lonchería"))
                .andExpect(jsonPath("$[0].precio").value(comparesEqualTo(new BigDecimal("45.00")), BigDecimal.class))
                .andExpect(jsonPath("$[0].descripcion").value("Jamón con queso y verduras."))
                .andExpect(jsonPath("$[*].*", hasSize(35)))
                .andExpect(jsonPath("$[*].precio", everyItem(isA(Number.class))))
                .andExpect(jsonPath("$[*].precioCentavos").isEmpty())
                .andExpect(jsonPath("$[*].categoriaId").isEmpty())
                .andExpect(jsonPath("$[*].activo").isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"comida, Comida / Lonchería, 2", "bebidas, Cafetería y Bebidas, 3", "tienda, Tienda y Snacks, 2"})
    void filtersByEachStoredCategory(String slug, String name, int count) throws Exception {
        mvc.perform(get("/api/productos").param("categoria", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(count)))
                .andExpect(jsonPath("$[*].categoria", everyItem(is(name))));
    }

    @Test
    @Transactional
    void validCategoryWithoutActiveProductsReturnsEmptyArray() throws Exception {
        jdbc.update("UPDATE productos SET activo = 0 WHERE categoria_id = ?", 1);
        mvc.perform(get("/api/productos").param("categoria", "comida"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(5)));
    }

    @Test
    @Transactional
    void usesCategoryNameAndSlugFromSqlite() throws Exception {
        jdbc.update("UPDATE categorias SET nombre = ?, slug = ? WHERE id = ?", "Lonchería actualizada", "loncheria", 1);
        mvc.perform(get("/api/productos").param("categoria", "loncheria"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].categoria", everyItem(is("Lonchería actualizada"))));
        mvc.perform(get("/api/productos").param("categoria", "comida"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Transactional
    void serializesFractionalPriceAsExactDecimalNumber() throws Exception {
        jdbc.update("UPDATE productos SET precio_centavos = ? WHERE id = ?", 4501, 1);
        mvc.perform(get("/api/productos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].precio").value(comparesEqualTo(new BigDecimal("45.01")), BigDecimal.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "desconocida", "COMIDA", " comida ", "comida' OR 1=1 --", "comida,bebidas"})
    void invalidCategoryReturnsControlledBadRequest(String slug) throws Exception {
        mvc.perform(get("/api/productos").param("categoria", slug))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_INVALIDA"))
                .andExpect(jsonPath("$.mensaje").isNotEmpty())
                .andExpect(jsonPath("$.*", hasSize(2)));
    }

    @Test
    void repeatedCategoryParameterIsRejected() throws Exception {
        mvc.perform(get("/api/productos").param("categoria", "comida", "bebidas"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CATEGORIA_INVALIDA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT", "PATCH", "DELETE"})
    void writeMethodsWithoutCsrfAreForbidden(String method) throws Exception {
        for (String endpoint : new String[]{"/api/categorias", "/api/productos"}) {
            mvc.perform(request(org.springframework.http.HttpMethod.valueOf(method), endpoint))
                    .andExpect(status().isForbidden());
        }
    }
}
