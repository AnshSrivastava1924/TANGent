package com.tangent.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc unit tests for {@link SystemController}. This controller has no service dependencies to
 * mock; only its constructor-injected configuration values vary between tests.
 */
class SystemControllerTest {

    @Test
    void should_returnUpStatus_when_healthEndpointCalled() throws Exception {
        MockMvc mockMvc = mockMvcFor("", "");

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.service").value("TANGent"));
    }

    @Test
    void should_reportNotConfigured_when_noApiKeysPresent() throws Exception {
        MockMvc mockMvc = mockMvcFor("", "");

        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.massiveConfigured").value(false))
                .andExpect(jsonPath("$.data.alphaVantageConfigured").value(false))
                .andExpect(jsonPath("$.data.realtimeProvider").value("Not configured"));
    }

    @Test
    void should_reportMassiveConfigured_when_massiveKeyPresent() throws Exception {
        MockMvc mockMvc = mockMvcFor("massive-key", "");

        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.massiveConfigured").value(true))
                .andExpect(jsonPath("$.data.alphaVantageConfigured").value(false))
                .andExpect(jsonPath("$.data.realtimeProvider").value("Massive"));
    }

    @Test
    void should_reportAlphaVantageConfigured_when_onlyAlphaKeyPresent() throws Exception {
        MockMvc mockMvc = mockMvcFor("", "alpha-key");

        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.massiveConfigured").value(false))
                .andExpect(jsonPath("$.data.alphaVantageConfigured").value(true))
                .andExpect(jsonPath("$.data.realtimeProvider").value("Alpha Vantage"));
    }

    @Test
    void should_preferMassiveProvider_when_bothKeysPresent() throws Exception {
        MockMvc mockMvc = mockMvcFor("massive-key", "alpha-key");

        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.realtimeProvider").value("Massive"));
    }

    @Test
    void should_treatBlankKeys_asNotConfigured() throws Exception {
        MockMvc mockMvc = mockMvcFor("   ", "   ");

        mockMvc.perform(get("/api/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.massiveConfigured").value(false))
                .andExpect(jsonPath("$.data.alphaVantageConfigured").value(false));
    }

    private MockMvc mockMvcFor(String massiveKey, String alphaKey) {
        SystemController controller = new SystemController(massiveKey, alphaKey);
        return MockMvcBuilders.standaloneSetup(controller).build();
    }
}

