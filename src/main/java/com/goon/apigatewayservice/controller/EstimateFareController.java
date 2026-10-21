package com.goon.apigatewayservice.controller;

import com.goon.apigatewayservice.dto.FareEstimateRequest;
import com.goon.apigatewayservice.dto.FareEstimateResponse;
import com.goon.apigatewayservice.service.PricingClientService;
import com.goon.grpc.pricing.FareQuoteRequest;
import com.goon.grpc.pricing.FareQuoteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fares")
@RequiredArgsConstructor
public class EstimateFareController {

    private final PricingClientService pricingClientService;

    @PostMapping("/estimate")
    public ResponseEntity<FareEstimateResponse> estimateFare(
            @RequestBody FareEstimateRequest request,
            Authentication authentication) {

        FareQuoteRequest grpcRequest = FareQuoteRequest.newBuilder()
                .setCustomerId(authentication.getName())
                .setPickupLat(request.pickupLat())
                .setPickupLon(request.pickupLon())
                .setDropLat(request.dropLat())
                .setDropLon(request.dropLon())
                .setVehicleType(request.vehicleType())
                .build();

        FareQuoteResponse grpcResponse =
                pricingClientService.estimateFare(grpcRequest);

        FareEstimateResponse response = new FareEstimateResponse(
                grpcResponse.getEstimatedFare(),
                grpcResponse.getCurrency(),
                grpcResponse.getSignedQuote()
        );

        return ResponseEntity.ok(response);
    }
}