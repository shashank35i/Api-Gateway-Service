package com.goon.apigatewayservice.security;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class InternalServiceTokenProvider {

    private static final String PRINCIPAL_NAME = "api-gateway";

    private final OAuth2AuthorizedClientManager authorizedClientManager;
    private final ConcurrentMap<String, OAuth2AccessToken> cache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Object> locks = new ConcurrentHashMap<>();

    public InternalServiceTokenProvider(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    public String getAccessToken(String registrationId) {
        OAuth2AccessToken token = cache.get(registrationId);
        if (valid(token)) return token.getTokenValue();

        Object lock = locks.computeIfAbsent(registrationId, ignored -> new Object());
        synchronized (lock) {
            token = cache.get(registrationId);
            if (valid(token)) return token.getTokenValue();

            OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                    .withClientRegistrationId(registrationId)
                    .principal(PRINCIPAL_NAME)
                    .build();
            OAuth2AuthorizedClient client = authorizedClientManager.authorize(request);
            if (client == null || client.getAccessToken() == null) {
                throw new IllegalStateException("Could not obtain OAuth2 access token for " + registrationId);
            }
            token = client.getAccessToken();
            cache.put(registrationId, token);
            return token.getTokenValue();
        }
    }

    private boolean valid(OAuth2AccessToken token) {
        return token != null && token.getExpiresAt() != null
                && token.getExpiresAt().isAfter(Instant.now().plusSeconds(30));
    }
}
