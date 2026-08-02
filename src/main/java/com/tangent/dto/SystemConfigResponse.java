package com.tangent.dto;

public record SystemConfigResponse(boolean massiveConfigured, boolean alphaVantageConfigured,
                                   String realtimeProvider) {
}
