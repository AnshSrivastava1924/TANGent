package com.tangent.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "TANGent");
    }

    @GetMapping("/config")
    @SecurityRequirements
    @Operation(summary = "Show configured market-data providers", security = {})
    public Map<String, Object> config() {
        boolean massive = !massiveKey.isBlank();
        boolean alpha = !alphaKey.isBlank();
        return Map.of(
                "massiveConfigured", massive,
                "alphaVantageConfigured", alpha,
                "realtimeProvider", massive ? "Massive" : alpha ? "Alpha Vantage" : "Demo fallback"
        );
    }
}
