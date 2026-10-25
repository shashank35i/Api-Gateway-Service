package com.goon.apigatewayservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class UserIdentityFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain
    ) throws ServletException, IOException {

        var auth = SecurityContextHolder.getContext()
                .getAuthentication();

        String userId = auth != null
                && auth.isAuthenticated()
                && !auth.getAuthorities().isEmpty()
                ? auth.getName() : null;

        HttpServletRequestWrapper wrapped =
                new HttpServletRequestWrapper(request) {

                    @Override
                    public String getHeader(String name) {
                        if ("X-User-Id".equalsIgnoreCase(name)) {
                            return userId;
                        }
                        return super.getHeader(name);
                    }

                    @Override
                    public Enumeration<String> getHeaders(String name) {
                        if ("X-User-Id".equalsIgnoreCase(name)) {
                            return Collections.enumeration(
                                    userId == null
                                            ? List.of()
                                            : List.of(userId)
                            );
                        }
                        return super.getHeaders(name);
                    }

                    @Override
                    public Enumeration<String> getHeaderNames() {
                        List<String> names = Collections.list(
                                super.getHeaderNames()
                        );

                        names.removeIf(
                                name -> "X-User-Id".equalsIgnoreCase(name)
                        );

                        if (userId != null) {
                            names.add("X-User-Id");
                        }

                        return Collections.enumeration(names);
                    }
                };

        chain.doFilter(wrapped, response);
    }
}