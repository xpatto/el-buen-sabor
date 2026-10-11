package com.elbuensabor.menu.security;

import com.elbuensabor.menu.SqliteIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class FrontendTests extends SqliteIntegrationTest {
    @Autowired MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"/css/styles.css","/css/admin.css","/js/app.js","/js/admin.js","/js/catalog.js"})
    void onlyOwnAssetsArePublicWithStrictCsp(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy",
                        "default-src 'self'; script-src 'self'; style-src 'self'; object-src 'none'; base-uri 'none'; form-action 'self'; frame-ancestors 'none'"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"));
        mvc.perform(post(path).with(csrf())).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/css/otro.css","/js/otro.js","/js/admin.js.map"})
    void unlistedAssetsRemainDenied(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/","/index.html"})
    void menuIsPublicWithoutAdminAccessLinkAndUsesExternalAssets(String path) throws Exception {
        if (path.equals("/")) {
            // MockMvc registra el forward, pero no ejecuta el segundo despacho del contenedor.
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(forwardedUrl("index.html"));
            path = "/index.html";
        }
        mvc.perform(get(path)).andExpect(status().isOk())
                .andExpect(content().string(containsString("El Buen Sabor")))
                .andExpect(content().string(not(containsString("/admin"))))
                .andExpect(content().string(containsString("id=\"category-filter\"")))
                .andExpect(content().string(containsString("id=\"products\"")))
                .andExpect(content().string(containsString("src=\"/js/app.js\"")))
                .andExpect(content().string(containsString("role=\"status\"")));
    }

    @Test
    void anonymousGetsLoginAndNotTheEditorOrAjaxToken() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("name=\"password\"")))
                .andExpect(content().string(not(containsString("id=\"product-form\""))))
                .andExpect(content().string(not(containsString("name=\"csrf-token\""))));
    }

    @Test
    @Transactional
    void authenticatedPageExposesUsableAjaxTokenAndLogoutRemainsProtected() throws Exception {
        var session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session).with(csrf())
                        .param("username", TestAdminCredentials.USERNAME).param("password", TestAdminCredentials.PASSWORD))
                .andExpect(status().isFound());
        var page = mvc.perform(get("/admin").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"product-form\"")))
                .andExpect(content().string(containsString("id=\"admin-products\"")))
                .andExpect(content().string(containsString("src=\"/js/admin.js\"")))
                .andExpect(content().string(not(containsString("name=\"password\"")))).andReturn();
        var matcher = java.util.regex.Pattern.compile("name=\"csrf-token\" content=\"([^\"]+)\"")
                .matcher(page.getResponse().getContentAsString());
        assertThat(matcher.find()).isTrue();
        mvc.perform(post("/api/admin/productos").session(session).header("X-CSRF-TOKEN", matcher.group(1))
                        .contentType("application/json").content("""
                                {"nombre":"AJAX","descripcion":"Prueba","precio":12.34,"categoriaId":1}
                                """)).andExpect(status().isCreated());
        mvc.perform(post("/admin/logout").session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/admin/logout").session(session).header("X-CSRF-TOKEN", matcher.group(1)))
                .andExpect(status().isFound());
        assertThat(session.isInvalid()).isTrue();
    }
}
