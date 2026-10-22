package com.goon.apigatewayservice.security;

import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.stereotype.Component;

@Component
public class InternalServiceTokenProvider {

    private static final String REGISTRATION_ID = "goon-internal";
    private static final String PRINCIPAL_NAME = "api-gateway";

    private final OAuth2AuthorizedClientManager authorizedClientManager;

    public InternalServiceTokenProvider(OAuth2AuthorizedClientManager authorizedClientManager) {
        this.authorizedClientManager = authorizedClientManager;
    }

    public String getAccessToken() {
        OAuth2AuthorizeRequest request = OAuth2AuthorizeRequest
                .withClientRegistrationId(REGISTRATION_ID)
                .principal(PRINCIPAL_NAME)
                .build();
        OAuth2AuthorizedClient client = authorizedClientManager.authorize(request);
        if (client == null || client.getAccessToken() == null) {
            throw new IllegalStateException("Could not obtain GoOn internal access token");
        }
        return client.getAccessToken().getTokenValue();
    }
}
