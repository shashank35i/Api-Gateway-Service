package com.goon.apigatewayservice.service;

import com.goon.grpc.pricing.PricingServiceGrpc;
import org.springframework.stereotype.Service;

@Service
public class PricingClientService {


    private PricingServiceGrpc.PricingServiceBlockingStub pricingServiceBlockingStub;

}
