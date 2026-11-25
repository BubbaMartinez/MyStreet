package com.mystreet.config;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

import com.mystreet.security.JwtAuthenticationFilter;
import com.mystreet.security.JwtService;
import com.mystreet.security.SecurityErrorHandler;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http,
                                    JwtService jwtService,
                                    SecurityErrorHandler errorHandler) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.sameOrigin())) // consola H2 en dev
            .exceptionHandling(e -> e
                    .authenticationEntryPoint(errorHandler)
                    .accessDeniedHandler(errorHandler))
            // antMatcher evita la ambigüedad de MVC cuando la consola H2 registra otro servlet
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(antMatcher(HttpMethod.OPTIONS, "/**")).permitAll()
                    .requestMatchers(antMatcher("/error")).permitAll()
                    .requestMatchers(antMatcher("/api/auth/**")).permitAll()
                    .requestMatchers(antMatcher("/swagger-ui.html"),
                                     antMatcher("/swagger-ui/**"),
                                     antMatcher("/v3/api-docs/**")).permitAll()
                    .requestMatchers(antMatcher("/h2-console/**")).permitAll()
                    .requestMatchers(antMatcher(HttpMethod.GET, "/api/products"),
                                     antMatcher(HttpMethod.GET, "/api/products/**")).permitAll()
                    .requestMatchers(antMatcher(HttpMethod.POST, "/api/products"),
                                     antMatcher(HttpMethod.PUT, "/api/products/**"),
                                     antMatcher(HttpMethod.DELETE, "/api/products/**")).hasRole("ADMIN")
                    .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwtService),
                             UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:4200"));
        cfg.setAllowedMethods(List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
