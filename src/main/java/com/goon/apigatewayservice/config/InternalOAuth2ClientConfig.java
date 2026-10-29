package com.goon.apigatewayservice.config;

import com.goon.apigatewayservice.security.InternalServiceTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelBuilderCustomizer;
import org.springframework.grpc.client.interceptor.security.BearerTokenAuthenticationInterceptor;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.function.Supplier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class InternalOAuth2ClientConfig {

    private static final Logger log = LoggerFactory.getLogger(InternalOAuth2ClientConfig.class);

    private static final String SERVICE_PRINCIPAL = "api-gateway";
    private final Map<String, Object> authorizationLocks = new ConcurrentHashMap<>();

    @Bean
    org.springframework.cloud.gateway.server.mvc.filter.FilterSupplier serviceTokenRelayFilterSupplier() {
        return new ServiceTokenRelayFilterFunctions.FilterSupplier();
    }

    @Bean
    OAuth2AuthorizedClientManager internalAuthorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService authorizedClients) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, authorizedClients);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build());

        // TokenRelay calls this manager with the incoming HTTP user's principal.
        // Client-credentials tokens belong to the gateway service, so normalize
        // that principal and serialize cache misses independently per registration.
        return request -> {
            String registrationId = request.getClientRegistrationId();
            Object lock = authorizationLocks.computeIfAbsent(registrationId, ignored -> new Object());
            synchronized (lock) {
                var previous = authorizedClients.loadAuthorizedClient(registrationId, SERVICE_PRINCIPAL);
                var serviceRequest = org.springframework.security.oauth2.client.OAuth2AuthorizeRequest
                        .withClientRegistrationId(registrationId)
                        .principal(SERVICE_PRINCIPAL)
                        .attributes(attributes -> attributes.putAll(request.getAttributes()))
                        .build();
                var authorized = manager.authorize(serviceRequest);
                OAuth2AccessToken before = previous == null ? null : previous.getAccessToken();
                OAuth2AccessToken after = authorized == null ? null : authorized.getAccessToken();
                if (after != null && (before == null || !after.getIssuedAt().equals(before.getIssuedAt()))) {
                    log.info("TOKEN_FETCH registrationId={} principal={}", registrationId, SERVICE_PRINCIPAL);
                }
                return authorized;
            }
        };
    }

    @Bean
    GrpcChannelBuilderCustomizer<?> pricingServiceBearerToken(
            InternalServiceTokenProvider tokenProvider) {
        Supplier<String> accessToken = () -> tokenProvider.getAccessToken("goon-internal");
        return GrpcChannelBuilderCustomizer.matching("pricing", builder ->
                builder.intercept(new BearerTokenAuthenticationInterceptor(accessToken)));
    }
}
