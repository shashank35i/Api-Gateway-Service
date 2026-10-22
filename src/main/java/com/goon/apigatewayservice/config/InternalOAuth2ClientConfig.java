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

@Configuration
public class InternalOAuth2ClientConfig {

    @Bean
    OAuth2AuthorizedClientManager internalAuthorizedClientManager(
            ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientService authorizedClients) {
        AuthorizedClientServiceOAuth2AuthorizedClientManager manager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(registrations, authorizedClients);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build());
        return manager;
    }

    @Bean
    GrpcChannelBuilderCustomizer<?> pricingServiceBearerToken(
            InternalServiceTokenProvider tokenProvider) {
        Supplier<String> accessToken = tokenProvider::getAccessToken;
        return GrpcChannelBuilderCustomizer.matching("pricing", builder ->
                builder.intercept(new BearerTokenAuthenticationInterceptor(accessToken)));
    }
}
