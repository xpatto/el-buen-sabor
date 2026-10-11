package com.elbuensabor.menu.controller;

import java.util.Map;
import java.util.stream.Stream;

import com.elbuensabor.menu.SqliteIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
@Transactional
class AdminProductoApiTests extends SqliteIntegrationTest {

    private static final String ROOT = "/api/admin/productos";
    private static final String VALID = """
            {"nombre":"Especial","descripcion":"Con queso","precio":12.34,"categoriaId":1}
            """;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;

    @Test
    void actualLoginSessionAndRenderedCsrfCanCreateProduct() throws Exception {
        var page = mvc.perform(get("/admin")).andExpect(status().isOk()).andReturn();
        var session = (org.springframework.mock.web.MockHttpSession) page.getRequest().getSession(false);
        mvc.perform(post("/admin/login").session(session)
                        .param("_csrf", renderedToken(page.getResponse().getContentAsString()))
                        .param("username", com.elbuensabor.menu.security.TestAdminCredentials.USERNAME)
                        .param("password", com.elbuensabor.menu.security.TestAdminCredentials.PASSWORD))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/admin"));
        var authenticated = mvc.perform(get("/admin").session(session)).andExpect(status().isOk()).andReturn();
        mvc.perform(post(ROOT).session(session)
                        .header("X-CSRF-TOKEN", renderedToken(authenticated.getResponse().getContentAsString()))
                        .contentType("application/json").content(VALID))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.nombre").value("Especial"));
    }

    private String renderedToken(String html) {
        var matcher = java.util.regex.Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"").matcher(html);
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @Test
    void listsAllProductsOrderedIncludingInactiveAndExactPrice() throws Exception {
        jdbc.update("UPDATE productos SET activo=0, precio_centavos=? WHERE id=1", Long.MAX_VALUE);
        mvc.perform(get(ROOT).with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[*].id", contains(1, 2, 3, 4, 5, 6, 7)))
                .andExpect(jsonPath("$[0].activo").value(false))
                .andExpect(jsonPath("$[0].categoriaId").value(1))
                .andExpect(jsonPath("$[0].categoria").value("Comida / Lonchería"))
                .andExpect(jsonPath("$[0].nombre").value("Torta de jamón"))
                .andExpect(jsonPath("$[0].descripcion").isString())
                .andExpect(content().string(containsString("92233720368547758.07")));
    }

    @Test
    void createEditHideAndReactivatePreservePublicContract() throws Exception {
        var created = mvc.perform(post(ROOT).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.precio").value(12.34))
                .andReturn();
        long id = mapper.readTree(created.getResponse().getContentAsString()).path("id").asLong();
        assertThat(id).isGreaterThan(7);
        assertThat(created.getResponse().getHeader("Location")).isEqualTo(ROOT + "/" + id);
        mvc.perform(patch(ROOT + "/" + id + "/estado").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content("{\"activo\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(false));
        mvc.perform(put(ROOT + "/" + id).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content("""
                                {"nombre":"Editado","descripcion":"Otra descripción","precio":0.01,"categoriaId":2,"activo":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Editado"))
                .andExpect(jsonPath("$.descripcion").value("Otra descripción"))
                .andExpect(jsonPath("$.precio").value(0.01))
                .andExpect(jsonPath("$.categoriaId").value(2))
                .andExpect(jsonPath("$.categoria").value("Cafetería y Bebidas"))
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(get("/api/productos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem((int) id))));
        mvc.perform(patch(ROOT + "/" + id + "/estado").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content("{\"activo\":true,\"precio\":999}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.precio").value(0.01));
        mvc.perform(get("/api/productos?categoria=bebidas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].nombre", contains("Editado")))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].precio", contains(0.01)))
                .andExpect(jsonPath("$[?(@.id == " + id + ")].activo").isEmpty())
                .andExpect(jsonPath("$[?(@.id == " + id + ")].categoriaId").isEmpty());
        assertThat(jdbc.queryForObject("SELECT precio_centavos FROM productos WHERE id=?", Long.class, id)).isEqualTo(1L);
        var second = mvc.perform(post(ROOT).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(VALID))
                .andExpect(status().isCreated()).andReturn();
        assertThat(mapper.readTree(second.getResponse().getContentAsString()).path("id").asLong()).isGreaterThan(id);
    }

    static Stream<String> invalidProductBodies() {
        return Stream.of(
                "{}", "null",
                "{\"nombre\":null,\"descripcion\":\"D\",\"precio\":1,\"categoriaId\":1}",
                "{\"nombre\":\" \",\"descripcion\":\"D\",\"precio\":1,\"categoriaId\":1}",
                "{\"nombre\":\"N\",\"descripcion\":null,\"precio\":1,\"categoriaId\":1}",
                "{\"nombre\":\"N\",\"descripcion\":\" \",\"precio\":1,\"categoriaId\":1}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":null,\"categoriaId\":1}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":0,\"categoriaId\":1}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":-1,\"categoriaId\":1}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":1.001,\"categoriaId\":1}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":1.000,\"categoriaId\":1}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":92233720368547758.08,\"categoriaId\":1}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":1e100,\"categoriaId\":1}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":1e-100,\"categoriaId\":1}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":1,\"categoriaId\":null}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":1,\"categoriaId\":0}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":1,\"categoriaId\":-1}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":1,\"categoriaId\":99999}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":1,\"categoriaId\":1.9}",
                "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":1,\"categoriaId\":\"1\"}",
                "{\"nombre\":\"" + "N".repeat(121) + "\",\"descripcion\":\"D\",\"precio\":1,\"categoriaId\":1}",
                "{\"nombre\":\"N\",\"descripcion\":\"" + "D".repeat(2001) + "\",\"precio\":1,\"categoriaId\":1}",
                "{", "{\"nombre\":\"N\",\"descripcion\":\"D\",\"precio\":\"no-numero\",\"categoriaId\":1}");
    }

    @ParameterizedTest
    @MethodSource("invalidProductBodies")
    void invalidCreateAndUpdateReturn400AndLeaveDataUntouched(String body) throws Exception {
        var before = jdbc.queryForList("SELECT * FROM productos ORDER BY id");
        for (HttpMethod method : new HttpMethod[]{HttpMethod.POST, HttpMethod.PUT}) {
            mvc.perform(request(method, method == HttpMethod.POST ? ROOT : ROOT + "/1")
                            .with(user("admin").roles("ADMIN")).with(csrf())
                            .contentType("application/json").content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").isString())
                    .andExpect(jsonPath("$.mensaje").isString())
                    .andExpect(content().string(not(containsString("jdbc:sqlite"))));
        }
        assertThat(jdbc.queryForList("SELECT * FROM productos ORDER BY id")).isEqualTo(before);
    }

    @Test
    void acceptsExactMaximumAndTextLimitsAndParameterizedStrings() throws Exception {
        String name = "N".repeat(120);
        String description = "D".repeat(1996) + "';--";
        String body = mapper.writeValueAsString(Map.of("nombre", name, "descripcion", description,
                "precio", new java.math.BigDecimal("92233720368547758.07"), "categoriaId", 1));
        var response = mvc.perform(post(ROOT).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value(name))
                .andExpect(jsonPath("$.descripcion").value(description))
                .andExpect(content().string(containsString("92233720368547758.07"))).andReturn();
        long id = mapper.readTree(response.getResponse().getContentAsString()).path("id").asLong();
        assertThat(jdbc.queryForObject("SELECT precio_centavos FROM productos WHERE id=?", Long.class, id))
                .isEqualTo(Long.MAX_VALUE);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "null", "{\"activo\":null}", "{\"activo\":\"true\"}", "{\"activo\":1}",
            "{\"activo\":0}", "{\"activo\":[]}", "{\"activo\":{}}"})
    void stateMustBeAnActualRequiredJsonBoolean(String body) throws Exception {
        mvc.perform(patch(ROOT + "/1/estado").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").isString());
        assertThat(jdbc.queryForObject("SELECT activo FROM productos WHERE id=1", Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "abc", "9223372036854775808"})
    void invalidPathIdsReturn400(String id) throws Exception {
        mvc.perform(delete(ROOT + "/" + id).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isBadRequest());
        mvc.perform(put(ROOT + "/" + id).with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(VALID)).andExpect(status().isBadRequest());
        mvc.perform(patch(ROOT + "/" + id + "/estado").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content("{\"activo\":false}")).andExpect(status().isBadRequest());
    }

    @Test
    void missingProductsReturn404() throws Exception {
        mvc.perform(put(ROOT + "/99999").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(VALID))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("PRODUCTO_NO_ENCONTRADO"));
        mvc.perform(patch(ROOT + "/99999/estado").with(user("admin").roles("ADMIN")).with(csrf())
                        .contentType("application/json").content("{\"activo\":false}"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("PRODUCTO_NO_ENCONTRADO"));
        mvc.perform(delete(ROOT + "/99999").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("PRODUCTO_NO_ENCONTRADO"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
    void allOperationsRequireAdminAndWritesRequireValidCsrf(String method) throws Exception {
        String path = switch (method) {
            case "PUT", "DELETE" -> ROOT + "/1";
            case "PATCH" -> ROOT + "/1/estado";
            default -> ROOT;
        };
        String body = method.equals("PATCH") ? "{\"activo\":false}" : VALID;
        var before = jdbc.queryForList("SELECT * FROM productos ORDER BY id");
        mvc.perform(request(HttpMethod.valueOf(method), path).with(csrf())
                        .contentType("application/json").content(body))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
        mvc.perform(request(HttpMethod.valueOf(method), path).with(user("lector").roles("USER")).with(csrf())
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
        if (!method.equals("GET")) {
            mvc.perform(request(HttpMethod.valueOf(method), path).with(user("admin").roles("ADMIN"))
                            .contentType("application/json").content(body)).andExpect(status().isForbidden());
            mvc.perform(request(HttpMethod.valueOf(method), path).with(user("admin").roles("ADMIN"))
                            .with(csrf().useInvalidToken()).contentType("application/json").content(body))
                    .andExpect(status().isForbidden());
            mvc.perform(request(HttpMethod.valueOf(method), path)
                            .contentType("application/json").content(body)).andExpect(status().isForbidden());
        }
        assertThat(jdbc.queryForList("SELECT * FROM productos ORDER BY id")).isEqualTo(before);
    }
    @Test
    void deleteRemovesOnlyRequestedProductAndReturnsEmpty204() throws Exception {
        var others = jdbc.queryForList("SELECT * FROM productos WHERE id <> 1 ORDER BY id");
        var categories = jdbc.queryForList("SELECT * FROM categorias ORDER BY id");
        mvc.perform(delete(ROOT + "/1").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM productos WHERE id=1", Integer.class)).isZero();
        assertThat(jdbc.queryForList("SELECT * FROM productos ORDER BY id")).isEqualTo(others);
        assertThat(jdbc.queryForList("SELECT * FROM categorias ORDER BY id")).isEqualTo(categories);
        mvc.perform(get("/api/productos")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(1))));
        mvc.perform(get(ROOT).with(user("admin").roles("ADMIN"))).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", not(hasItem(1))));
        mvc.perform(delete(ROOT + "/1").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCannotBeUsedOnPublicApiOrCategories() throws Exception {
        var before = jdbc.queryForList("SELECT * FROM productos ORDER BY id");
        for (String path : new String[]{"/api/productos/1", "/api/categorias/1"}) {
            mvc.perform(delete(path).with(user("admin").roles("ADMIN")).with(csrf()))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(delete("/api/admin/categorias/1").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNotFound());
        assertThat(jdbc.queryForList("SELECT * FROM productos ORDER BY id")).isEqualTo(before);
    }

}
