package com.goon.apigatewayservice.config;

import com.goon.apigatewayservice.security.JwtAuthenticationFilter;
import com.goon.apigatewayservice.security.UserIdentityFilter;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserIdentityFilter userIdentityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        return http
                .cors(cors -> {})
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        ))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((req, res, ex) ->
                                res.sendError(
                                        HttpServletResponse.SC_UNAUTHORIZED
                                ))
                        .accessDeniedHandler((req, res, ex) ->
                                res.sendError(
                                        HttpServletResponse.SC_FORBIDDEN
                                )))
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST, "/webhooks/razorpay").permitAll()
                        .requestMatchers(HttpMethod.GET, "/payment-checkout.html").permitAll()
                        .requestMatchers("/payments/**").hasRole("CUSTOMER")

                        .requestMatchers("/auth/**", "/actuator/health")
                        .permitAll()

                        .requestMatchers(HttpMethod.POST, "/fares/estimate")
                        .hasRole("CUSTOMER")

                        .requestMatchers(HttpMethod.POST, "/api/ingestion")
                        .hasRole("DRIVER")

                        .requestMatchers(
                                "/api/driver/**",
                                "/driver/**",
                                "/offers/**"
                        ).hasRole("DRIVER")

                        .requestMatchers(
                                "/api/customer/**",
                                "/customer/**",
                                "/rides/**"
                        ).hasRole("CUSTOMER")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .addFilterAfter(
                        userIdentityFilter,
                        JwtAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:63342",
                "http://localhost:5500"
        ));

        config.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));

        config.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Idempotency-Key"
        ));

        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
