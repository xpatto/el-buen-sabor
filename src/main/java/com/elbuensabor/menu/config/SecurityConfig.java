package com.elbuensabor.menu.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.PortMapper;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper mapper, Environment environment) throws Exception {
        var errors = new SecurityErrorHandler(mapper);
        http.authorizeHttpRequests(authorize -> authorize
                        // Solo despachos internos de error; GET /error continúa denegado.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.GET, "/", "/index.html", "/api/categorias", "/api/productos", "/admin").permitAll()
                        .requestMatchers(HttpMethod.POST, "/admin/login", "/admin/logout").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .csrf(Customizer.withDefaults())
                .httpBasic(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .formLogin(form -> form.loginPage("/admin").loginProcessingUrl("/admin/login")
                        .defaultSuccessUrl("/admin", true).failureUrl("/admin?error"))
                .logout(logout -> logout.logoutUrl("/admin/logout").logoutSuccessUrl("/admin?logout")
                        .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(fixation -> fixation.changeSessionId()))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; script-src 'none'; style-src 'self'; object-src 'none'; "
                                        + "base-uri 'none'; form-action 'self'; frame-ancestors 'none'"))
                        .httpStrictTransportSecurity(hsts -> hsts.maxAgeInSeconds(31536000).includeSubDomains(false)));
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            // El proxy publica HTTPS en 443; el puerto interno de Tomcat puede variar.
            http.portMapper(mapperConfig -> mapperConfig.portMapper(new PortMapper() {
                @Override
                public Integer lookupHttpsPort(Integer httpPort) {
                    return 443;
                }

                @Override
                public Integer lookupHttpPort(Integer httpsPort) {
                    return 80;
                }
            }));
            http.redirectToHttps(Customizer.withDefaults());
        }
        return http.build();
    }
}
