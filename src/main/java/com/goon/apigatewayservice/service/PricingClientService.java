package com.goon.apigatewayservice.service;

import com.goon.grpc.pricing.FareQuoteRequest;
import com.goon.grpc.pricing.FareQuoteResponse;
import com.goon.grpc.pricing.PricingServiceGrpc;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PricingClientService {


    private final PricingServiceGrpc.PricingServiceBlockingStub pricingServiceBlockingStub;

    public FareQuoteResponse estimateFare(FareQuoteRequest fareQuoteRequest){
        return pricingServiceBlockingStub
                .withDeadlineAfter(2, TimeUnit.SECONDS)
                .getFareQuote(fareQuoteRequest);
    }

}
