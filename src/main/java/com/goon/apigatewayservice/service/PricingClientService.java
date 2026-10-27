package com.goon.apigatewayservice.service;

import com.goon.grpc.pricing.FareQuoteRequest;
import com.goon.grpc.pricing.FareQuoteResponse;
import com.goon.grpc.pricing.PricingServiceGrpc;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PricingClientService {


    private final PricingServiceGrpc.PricingServiceBlockingStub pricingServiceBlockingStub;

    public FareQuoteResponse estimateFare(FareQuoteRequest request) {
        try {
            return pricingServiceBlockingStub
                    .withDeadlineAfter(10, TimeUnit.SECONDS)
                    .getFareQuote(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
                throw new ResponseStatusException(
                        HttpStatus.GATEWAY_TIMEOUT,
                        "Pricing service timed out"
                );
            }
            throw e;
        }
    }
}
