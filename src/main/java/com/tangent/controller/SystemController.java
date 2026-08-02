package com.tangent.controller;

import com.tangent.dto.HealthResponse;
import com.tangent.dto.SystemConfigResponse;
import com.tangent.wrapper.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SystemController {

    private final String massiveKey;
    private final String alphaKey;

    public SystemController(@Value("${market.massive.api-key:}") String massiveKey,
                            @Value("${market.alpha-vantage.api-key:}") String alphaKey) {
        this.massiveKey = massiveKey;
        this.alphaKey = alphaKey;
    }

    @GetMapping("/health")
    @SecurityRequirements
    @Operation(summary = "Check backend and database health", security = {})
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(new HealthResponse("UP", "TANGent"));
    }

    @GetMapping("/config")
    @SecurityRequirements
    @Operation(summary = "Show configured market-data providers", security = {})
    public ApiResponse<SystemConfigResponse> config() {
        boolean massive = !massiveKey.isBlank();
        boolean alpha = !alphaKey.isBlank();
        return ApiResponse.success(new SystemConfigResponse(massive, alpha,
                massive ? "Massive" : alpha ? "Alpha Vantage" : "Not configured"));
    }
}
