package com.theisraelayooluwa.absencebackend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.theisraelayooluwa.absencebackend.dto.ApiResponse;
import com.theisraelayooluwa.absencebackend.security.BearerTokenAuthenticationFilter;
import com.theisraelayooluwa.absencebackend.security.TokenService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                           AccessDeniedHandler accessDeniedHandler,
                                           AuthenticationEntryPoint authenticationEntryPoint,
                                           BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-ui/index.html",
                                "/webjars/**",
                                "/v1/api-docs",
                                "/v1/api-docs/**",
                                "/v3/api-docs/**",
                                "/error"
                                ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/employers/onboard", "/api/employers/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/employees/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/employees").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/absences/*/approve", "/api/absences/*/reject").hasAnyRole("MANAGER", "C_LEVEL_EXECUTIVE")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(bearerTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                );
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    BearerTokenAuthenticationFilter bearerTokenAuthenticationFilter(TokenService tokenService,
                                                                     org.springframework.security.core.userdetails.UserDetailsService userDetailsService) {
        return new BearerTokenAuthenticationFilter(tokenService, userDetailsService);
    }

    @Bean
    AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            new ObjectMapper().writeValue(response.getWriter(),
                    new ApiResponse<>(HttpStatus.FORBIDDEN.value(), accessDeniedException.getMessage(), null));
        };
    }

    @Bean
    AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            new ObjectMapper().writeValue(response.getWriter(),
                    new ApiResponse<>(HttpStatus.UNAUTHORIZED.value(), authException.getMessage(), null));
        };
    }
}
