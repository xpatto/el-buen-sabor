package com.elbuensabor.menu.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

/** Credenciales públicas exclusivamente para evaluación local y catálogo separado. */
@Configuration(proxyBeanMethods = false)
@Profile("demo")
public class DemoConfig {
    private static final String DATABASE_URL = "jdbc:sqlite:./data/menu-demo.db";

    @Bean
    static BeanFactoryPostProcessor demoSafetyChecks(Environment environment) {
        // Antes de instanciar DataSource o Tomcat: una configuración rechazada no abre SQLite.
        return beanFactory -> {
            String[] profiles = environment.getActiveProfiles();
            if (profiles.length == 0) profiles = environment.getDefaultProfiles();
            if (!Arrays.equals(profiles, new String[]{"demo"})) {
                throw new IllegalStateException("Demo debe ser el único perfil activo.");
            }
            Binder binder = Binder.get(environment);
            if (!"127.0.0.1".equals(binder.bind("server.address", String.class).orElse(""))) {
                throw new IllegalStateException("Demo requiere server.address=127.0.0.1; no se permite acceso desde la red.");
            }
            if (!DATABASE_URL.equals(binder.bind("spring.datasource.url", String.class).orElse(""))
                    || !DATABASE_URL.equals(binder.bind("spring.datasource.hikari.jdbc-url", String.class).orElse(DATABASE_URL))) {
                throw new IllegalStateException("Demo requiere su base independiente data/menu-demo.db.");
            }
            Path demo = Path.of("data", "menu-demo.db");
            Path normal = Path.of("data", "menu.db");
            try {
                if (Files.isSymbolicLink(demo) || (Files.exists(demo) && Files.exists(normal) && Files.isSameFile(demo, normal))) {
                    throw new IllegalStateException("Demo requiere su base independiente; no se admiten enlaces a otra base.");
                }
                Files.createDirectories(demo.getParent());
            } catch (IOException exception) {
                throw new IllegalStateException("No se pudo preparar el directorio de la base demo.", exception);
            }
        };
    }

    @Bean
    UserDetailsService demoUserDetailsService(PasswordEncoder encoder) {
        return new InMemoryUserDetailsManager(User.withUsername("admin")
                .password(encoder.encode("admin")).roles("ADMIN").build());
    }

    @Bean
    DemoLoopbackCheck demoLoopbackCheck() {
        return new DemoLoopbackCheck();
    }

    /** Comprueba también la dirección efectiva de la fábrica tras aplicar las propiedades Boot. */
    static final class DemoLoopbackCheck implements WebServerFactoryCustomizer<TomcatServletWebServerFactory>, Ordered {
        @Override
        public int getOrder() {
            return Ordered.LOWEST_PRECEDENCE;
        }

        @Override
        public void customize(TomcatServletWebServerFactory factory) {
            if (factory.getAddress() == null || !"127.0.0.1".equals(factory.getAddress().getHostAddress())) {
                throw new IllegalStateException("Demo requiere server.address=127.0.0.1 en Tomcat.");
            }
        }
    }
}
