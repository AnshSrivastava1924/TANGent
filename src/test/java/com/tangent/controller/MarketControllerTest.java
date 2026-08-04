package com.tangent.controller;

import com.tangent.exception.ApiException;
import com.tangent.exception.GlobalExceptionHandler;
import com.tangent.service.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc unit tests for {@link MarketController}.
 */
@ExtendWith(MockitoExtension.class)
class MarketControllerTest {

    @Mock
    private MarketDataService market;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MarketController controller = new MarketController(market);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_returnOkWithQuote_when_symbolIsValid() throws Exception {
        when(market.quote("AAPL")).thenReturn(Map.of("symbol", "AAPL", "price", 150.0));

        mockMvc.perform(get("/api/market/quote/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.symbol").value("AAPL"))
                .andExpect(jsonPath("$.data.price").value(150.0));

        verify(market).quote("AAPL");
    }

    @Test
    void should_returnServiceUnavailable_when_quoteThrowsApiException() throws Exception {
        when(market.quote("BADSYM")).thenThrow(
                new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Live market data is unavailable for BADSYM"));

        mockMvc.perform(get("/api/market/quote/BADSYM"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Live market data is unavailable for BADSYM"));
    }

    @Test
    void should_returnOkWithHistory_when_rangeProvided() throws Exception {
        when(market.history("AAPL", "1mo")).thenReturn(Map.of("symbol", "AAPL", "bars", java.util.List.of()));

        mockMvc.perform(get("/api/market/history/AAPL").param("range", "1mo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.symbol").value("AAPL"));

        verify(market).history("AAPL", "1mo");
    }

    @Test
    void should_useDefaultRange_when_historyRangeParamIsMissing() throws Exception {
        when(market.history(eq("AAPL"), eq("3mo"))).thenReturn(Map.of("symbol", "AAPL"));

        mockMvc.perform(get("/api/market/history/AAPL"))
                .andExpect(status().isOk());

        verify(market).history("AAPL", "3mo");
    }

    @Test
    void should_returnOkWithCompareSeries_when_symbolsProvided() throws Exception {
        when(market.compare("AAPL,MSFT", "3mo")).thenReturn(Map.of("series", java.util.List.of()));

        mockMvc.perform(get("/api/market/compare").param("symbols", "AAPL,MSFT").param("range", "3mo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.series").isArray());
    }

    @Test
    void should_useDefaultSymbolsAndRange_when_compareParamsAreMissing() throws Exception {
        when(market.compare(eq("AAPL,MSFT,NVDA"), eq("3mo"))).thenReturn(Map.of("series", java.util.List.of()));

        mockMvc.perform(get("/api/market/compare"))
                .andExpect(status().isOk());

        verify(market).compare("AAPL,MSFT,NVDA", "3mo");
    }

    @Test
    void should_returnOkWithNews_when_symbolProvided() throws Exception {
        when(market.news("AAPL")).thenReturn(Map.of("articles", java.util.List.of()));

        mockMvc.perform(get("/api/market/news").param("symbol", "AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.articles").isArray());
    }

    @Test
    void should_useEmptySymbol_when_newsSymbolParamIsMissing() throws Exception {
        when(market.news(eq(""))).thenReturn(Map.of("articles", java.util.List.of()));

        mockMvc.perform(get("/api/market/news"))
                .andExpect(status().isOk());

        verify(market).news("");
    }

    @Test
    void should_returnOkWithSearchResults_when_queryProvided() throws Exception {
        when(market.search("apple")).thenReturn(Map.of("results", java.util.List.of()));

        mockMvc.perform(get("/api/market/search").param("q", "apple"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.results").isArray());
    }

    @Test
    void should_useDefaultQuery_when_searchQueryParamIsMissing() throws Exception {
        when(market.search(eq("AAPL"))).thenReturn(Map.of("results", java.util.List.of()));

        mockMvc.perform(get("/api/market/search"))
                .andExpect(status().isOk());

        verify(market).search("AAPL");
    }

    @Test
    void should_returnInternalServerError_when_unexpectedExceptionThrown() throws Exception {
        when(market.quote(anyString())).thenThrow(new RuntimeException("unexpected"));

        mockMvc.perform(get("/api/market/quote/AAPL"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }
}

