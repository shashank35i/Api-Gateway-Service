package com.goon.apigatewayservice.dto;

public record FareEstimateResponse(
        long estimatedFare,
        String currency,
        String signedQuote
) {}