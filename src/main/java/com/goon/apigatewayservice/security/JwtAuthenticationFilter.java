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
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import javax.management.relation.Role;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final SecretKey key;

    public JwtAuthenticationFilter(
            @Value("${app.jwt-secret-key}") String jwtKey
    ){
        byte[] bytes = Base64.getDecoder().decode(jwtKey);
        this.key = Keys.hmacShaKeyFor(bytes);
    }


    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException
    {
        System.out.println("JWT FILTER EXECUTED");
        String header = request.getHeader("Authorization");
        if(header==null || !header.startsWith("Bearer ")){
            filterChain.doFilter(request,response);
            return;
        }

        try {
        String token = header.substring(7);
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();


            System.out.println("Subject: " + claims.getSubject());
            System.out.println("Role: " + claims.get("role"));
            System.out.println("Issuer: " + claims.getIssuer());
        if(!"goon-auth".equals(claims.getIssuer()) ||
                claims.getExpiration()==null){          //some jwts can accept no expiration . so its valid to be null at jwt verification using jwts.parser . so we checking seperately
            throw new JwtException("Invalid Token");  //for backend . but in frontend it can give internal server error , so we use catch block to beautify it  .
        }


        //jwt standard claims takes strings
        String userId = UUID.fromString(claims.getSubject()).toString();  //validating structure // 111111 using string because grpc doesnt have uuid type , so if convert to uuid , and pass, there we have to convert to string again .
            String role = claims.get("role",String.class);
            if (!"CUSTOMER".equals(role) && !"DRIVER".equals(role)) {
                throw new JwtException("Invalid role");
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    userId,                 // 2222222  using string because grpc doesnt have uuid type , so if convert to uuid , and pass, there we have to convert to string again .
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);


        }
        catch (JwtException | IllegalArgumentException e) {
            System.out.println("JWT ERROR: " + e.getMessage());
            SecurityContextHolder.clearContext();
            response.sendError(401, "Invalid Token");
            return;
        }

        filterChain.doFilter(request,response);
    }
}
