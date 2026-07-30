package com.smse.rubik_solver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.smse.rubik_solver.user.OAuth2LoginSuccessHandler;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http, OAuth2LoginSuccessHandler successHandler) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Publiczne: strona, statyka, solver, sprawdzenie statusu, webhook Stripe,
                        // Swagger oraz endpointy logowania.
                        .requestMatchers(
                                "/", "/index.html", "/script.js", "/styles.css", "/favicon.ico",
                                "/cube/**", "/api/me", "/api/stripe/webhook",
                                "/oauth2/**", "/login/**", "/logout",
                                "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Funkcje premium (i inne /api/**) wymagaja zalogowania.
                        .requestMatchers("/api/**").authenticated()
                        // Reszta (np. actuator, springdoc) - domyslnie dostepna.
                        .anyRequest().permitAll())
                .oauth2Login(oauth -> oauth.successHandler(successHandler))
                .logout(logout -> logout.logoutSuccessUrl("/").permitAll())
                // CSRF wylaczony swiadomie: ochrone przed CSRF daje ciasteczko sesji z
                // SameSite=Lax (patrz application.properties) - przegladarka nie wysyla go
                // przy cross-site POST, wiec nikt nie wywola /api/checkout w imieniu usera.
                // Webhook Stripe jest publiczny, ale chroniony podpisem. Gdyby doszly akcje
                // formularzowe zmieniajace stan konta, rozwazyc wlaczenie tokenow CSRF.
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
