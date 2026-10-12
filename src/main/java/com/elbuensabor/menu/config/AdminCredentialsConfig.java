package com.elbuensabor.menu.config;

import java.util.regex.Pattern;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration(proxyBeanMethods = false)
public class AdminCredentialsConfig {

    private static final Pattern BCRYPT = Pattern.compile("\\A\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}\\z");

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    @Profile("!demo")
    UserDetailsService adminUserDetailsService(Environment environment) {
        String username = environment.getProperty("ADMIN_USERNAME", "");
        String hash = environment.getProperty("ADMIN_PASSWORD_HASH", "");
        if (username.isBlank() || !username.equals(username.strip()) || !BCRYPT.matcher(hash).matches()) {
            // No interpolar valores de configuración en excepciones ni logs.
            throw new IllegalStateException("Configuración administrativa ausente o inválida; revise ADMIN_USERNAME y ADMIN_PASSWORD_HASH.");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username).password(hash).roles("ADMIN").build());
    }
}
