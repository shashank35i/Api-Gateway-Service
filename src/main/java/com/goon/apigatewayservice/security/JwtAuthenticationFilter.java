package com.goon.apigatewayservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey key;

    public JwtAuthenticationFilter(
            @Value("${app.jwt-secret-key}") String jwtKey) {
        this.key = Keys.hmacShaKeyFor(
                Base64.getDecoder().decode(jwtKey)
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return CorsUtils.isPreFlightRequest(request);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(header.substring(7))
                    .getPayload();

            if (!"goon-auth".equals(claims.getIssuer())
                    || claims.getExpiration() == null) {
                throw new JwtException("Invalid token");
            }

            String userId = UUID.fromString(
                    claims.getSubject()
            ).toString();

            String role = claims.get("role", String.class);

            if (!"CUSTOMER".equals(role)
                    && !"DRIVER".equals(role)) {
                throw new JwtException("Invalid role");
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    userId,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );

            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

        } catch (JwtException | IllegalArgumentException e) {
            SecurityContextHolder.clearContext();
            response.sendError(401, "Invalid Token");
            return;
        }

        filterChain.doFilter(request, response);
    }
}