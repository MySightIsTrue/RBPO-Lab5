package com.example.demo.security;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import org.springframework.context.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean SecurityFilterChain filterChain(HttpSecurity http, JwtService jwt, UserRepository users) throws Exception {
        return http.addFilterBefore(new JwtFilter(jwt, users), UsernamePasswordAuthenticationFilter.class)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/auth/login", "/auth/register", "/auth/refresh").permitAll()
                        .requestMatchers("/api/**").authenticated().anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, error) -> {
                            response.setHeader("WWW-Authenticate", "Bearer");
                            response.setStatus(401); response.setContentType("application/json");
                            response.getWriter().write("{\"status\":401,\"error\":\"Authentication required\"}");
                        })
                        .accessDeniedHandler((request, response, error) -> {
                            response.setStatus(403); response.setContentType("application/json");
                            response.getWriter().write("{\"status\":403,\"error\":\"Access denied\"}");
                        })).build();
    }

    @Bean CommandLineRunner editor(UserRepository users, UserService service,
                                  @Value("${EDITOR_PASSWORD:}") String password) {
        return args -> {
            if (!password.isBlank() && users.findByUsername("editor").isEmpty()) {
                User editor = new User(); editor.setUsername("editor"); editor.setEmail("editor@example.com");
                editor.setPassword(password); editor = service.create(editor);
                editor.setRole("EDITOR"); users.save(editor);
            }
        };
    }
}

