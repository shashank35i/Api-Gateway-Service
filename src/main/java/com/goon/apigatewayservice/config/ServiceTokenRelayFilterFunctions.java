package com.goon.apigatewayservice.config;

import com.goon.apigatewayservice.security.InternalServiceTokenProvider;
import org.springframework.cloud.gateway.server.mvc.common.Shortcut;
import org.springframework.cloud.gateway.server.mvc.filter.SimpleFilterSupplier;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.common.MvcUtils.getApplicationContext;

public abstract class ServiceTokenRelayFilterFunctions {

    private ServiceTokenRelayFilterFunctions() {
    }

    @Shortcut
    public static HandlerFilterFunction<ServerResponse, ServerResponse> serviceTokenRelay(String registrationId) {
        return (request, next) -> {
            InternalServiceTokenProvider tokenProvider =
                    getApplicationContext(request).getBean(InternalServiceTokenProvider.class);
            String accessToken = tokenProvider.getAccessToken(registrationId);
            ServerRequest authorizedRequest = ServerRequest.from(request)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .build();
            return next.handle(authorizedRequest);
        };
    }

    public static class FilterSupplier extends SimpleFilterSupplier {

        public FilterSupplier() {
            super(ServiceTokenRelayFilterFunctions.class);
        }
    }
}
