package com.tangent.config;

import com.tangent.security.JwtAuthenticationFilter;
import com.tangent.wrapper.ApiErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class SecurityConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter,
                                            ObjectMapper json) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    json.writeValue(response.getOutputStream(), ApiErrorResponse.of(
                            HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                            "A valid JWT is required"));
                }))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/index.html", "/app.js", "/styles.css", "/favicon.ico",
                                "/api/health", "/api/config", "/api/auth", "/api/auth/**",
                                "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**",
                                "/v3/api-docs.yaml")
                        .permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
