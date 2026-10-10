package com.elbuensabor.menu.security;

import com.elbuensabor.menu.config.AdminCredentialsConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(OutputCaptureExtension.class)
class AdminCredentialsConfigTests {

    private final ApplicationContextRunner application = new ApplicationContextRunner()
            .withUserConfiguration(AdminCredentialsConfig.class)
            .withPropertyValues("spring.profiles.active=prod", "ADMIN_USERNAME=", "ADMIN_PASSWORD_HASH=");

    @Test
    void missingProductionCredentialsPreventStartup() {
        application.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void missingUsernamePreventsStartupWithoutLoggingHash(CapturedOutput output) {
        application.withPropertyValues("ADMIN_PASSWORD_HASH=" + TestAdminCredentials.HASH)
                .run(context -> assertThat(context).hasFailed());
        assertThat(output.getAll()).doesNotContain(TestAdminCredentials.HASH);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "texto-plano", "$2a$03$invalid", "$2a$32$invalid", "{bcrypt}no-admitido"})
    void invalidProductionHashPreventsStartup(String hash, CapturedOutput output) {
        application.withPropertyValues("ADMIN_USERNAME=" + TestAdminCredentials.USERNAME, "ADMIN_PASSWORD_HASH=" + hash)
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalStateException.class);
                });
        if (!hash.isEmpty()) {
            assertThat(output.getAll()).doesNotContain(hash);
        }
    }

    @Test
    void acceptsCompatibleBcryptAndDefinesOnlyRequestedAdmin(CapturedOutput output) {
        application.withPropertyValues("ADMIN_USERNAME=" + TestAdminCredentials.USERNAME,
                        "ADMIN_PASSWORD_HASH=" + TestAdminCredentials.HASH)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var users = context.getBean(UserDetailsService.class);
                    var admin = users.loadUserByUsername(TestAdminCredentials.USERNAME);
                    assertThat(admin.getAuthorities()).extracting("authority").containsExactly("ROLE_ADMIN");
                    assertThat(context.getBean(BCryptPasswordEncoder.class).matches(TestAdminCredentials.PASSWORD, admin.getPassword()))
                            .isTrue();
                    assertThatThrownBy(() -> users.loadUserByUsername("user")).isInstanceOf(UsernameNotFoundException.class);
                });
        assertThat(output.getAll()).doesNotContain(TestAdminCredentials.HASH, TestAdminCredentials.PASSWORD);
    }
}
