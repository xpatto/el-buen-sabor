package com.elbuensabor.menu.security;

import com.elbuensabor.menu.SqliteIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class SecurityTests extends SqliteIntegrationTest {

    @Autowired
    MockMvc mvc;

    @ParameterizedTest
    @ValueSource(strings = {"/", "/index.html", "/api/categorias", "/api/productos", "/api/productos?categoria=comida"})
    void publicGetRemainsAccessible(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isOk());
    }

    @Test
    void adminRendersLoginWithCsrfAndSecurityHeaders() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isOk()).andExpect(view().name("admin"))
                .andExpect(content().string(containsString("/admin/login")))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
                .andExpect(content().string(not(containsString(TestAdminCredentials.HASH))))
                .andExpect(content().string(not(containsString(TestAdminCredentials.PASSWORD))));
    }

    @Test
    void loginChangesSessionIdAndShowsProvisionalAdminState() throws Exception {
        MockHttpSession session = new MockHttpSession();
        String originalId = session.getId();
        mvc.perform(post("/admin/login").session(session).with(csrf())
                        .param("username", TestAdminCredentials.USERNAME)
                        .param("password", TestAdminCredentials.PASSWORD))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/admin"))
                .andExpect(authenticated().withUsername(TestAdminCredentials.USERNAME).withRoles("ADMIN"));
        assertThat(session.getId()).isNotEqualTo(originalId);
        mvc.perform(get("/admin").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Administración provisional")))
                .andExpect(content().string(containsString("/admin/logout")))
                .andExpect(content().string(not(containsString("/admin/login"))));
    }

    @Test
    void renderedFormCsrfTokenAuthenticatesWithoutTestTokenInjection() throws Exception {
        var page = mvc.perform(get("/admin")).andExpect(status().isOk()).andReturn();
        var token = java.util.regex.Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"")
                .matcher(page.getResponse().getContentAsString());
        assertThat(token.find()).isTrue();
        mvc.perform(post("/admin/login").session((MockHttpSession) page.getRequest().getSession(false))
                        .param("_csrf", token.group(1))
                        .param("username", TestAdminCredentials.USERNAME).param("password", TestAdminCredentials.PASSWORD))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/admin")).andExpect(authenticated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin-test", "no-existe"})
    void incorrectCredentialsUseSameGenericFailure(String username) throws Exception {
        mvc.perform(post("/admin/login").with(csrf()).param("username", username).param("password", "incorrecta"))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/admin?error"))
                .andExpect(unauthenticated());
        mvc.perform(get("/admin?error"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Credenciales incorrectas.")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin/login", "/admin/logout", "/api/admin/productos"})
    void postWithoutCsrfIsForbidden(String path) throws Exception {
        mvc.perform(post(path)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void invalidCsrfCannotAuthenticate() throws Exception {
        mvc.perform(post("/admin/login").with(csrf().useInvalidToken())
                        .param("username", TestAdminCredentials.USERNAME).param("password", TestAdminCredentials.PASSWORD))
                .andExpect(status().isForbidden()).andExpect(unauthenticated());
    }

    @Test
    void anonymousAdminApiGetsJson401WithoutRedirectOrBasicChallenge() throws Exception {
        mvc.perform(get("/api/admin/productos"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(header().doesNotExist("WWW-Authenticate"));
        mvc.perform(post("/api/admin/productos").with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserWithoutRoleGetsJson403() throws Exception {
        mvc.perform(get("/api/admin/productos").with(user("lector").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.codigo").value("ACCESO_DENEGADO"));
    }

    @Test
    void adminPassesSecurityButUnimplementedApiStillReturns404() throws Exception {
        mvc.perform(get("/api/admin/productos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/productos").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/productos").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/desconocida", "/admin/editar", "/login", "/logout", "/error", "/css/no-autorizado.css"})
    void unknownPathsAreDeniedEvenForAdmin(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized());
        mvc.perform(get(path).with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
    }

    @Test
    void logoutRequiresCsrfAndInvalidatesAuthenticatedSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/admin/login").session(session).with(csrf())
                .param("username", TestAdminCredentials.USERNAME).param("password", TestAdminCredentials.PASSWORD))
                .andExpect(authenticated());
        mvc.perform(post("/admin/logout").session(session)).andExpect(status().isForbidden());
        assertThat(session.isInvalid()).isFalse();
        mvc.perform(post("/admin/logout").session(session).with(csrf()))
                .andExpect(status().isFound()).andExpect(redirectedUrl("/admin?logout"))
                .andExpect(unauthenticated()).andExpect(cookie().maxAge("JSESSIONID", 0));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/admin/productos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/admin?logout"))
                .andExpect(content().string(containsString("Sesión cerrada.")));
    }
}
