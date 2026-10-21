package com.goon.apigatewayservice.dto;

public record FareEstimateRequest(
        double pickupLat,
        double pickupLon,
        double dropLat,
        double dropLon,
        String vehicleType
) {}