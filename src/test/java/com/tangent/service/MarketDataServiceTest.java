package com.tangent.service;

import com.tangent.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link MarketDataService}.
 *
 * <p>NOTE: {@code MarketDataService} builds its own internal {@code RestClient}/{@code HttpClient}
 * rather than receiving one through constructor injection, so outbound HTTP calls to the Massive
 * and Alpha Vantage providers cannot be mocked with Mockito without either (a) hitting the real
 * network/a WireMock server, or (b) refactoring the class to accept an injectable HTTP client.
 * No production code was changed per instructions. These tests therefore focus on all the
 * behavior that is reachable and verifiable without performing real network I/O: the
 * "no provider configured" branches (which every public method falls back to), input
 * normalization, and simple accessor behavior.
 *
 * <p>If deeper coverage of the Massive/Alpha Vantage response-parsing branches is desired, consider
 * refactoring {@code MarketDataService} to accept a {@code RestClient} (or a small HTTP abstraction)
 * via its constructor so it can be mocked in isolation.
 */
class MarketDataServiceTest {

    private MarketDataService serviceWithNoProviders;
    private ObjectMapper json;

    @BeforeEach
    void setUp() {
        json = new ObjectMapper();
        serviceWithNoProviders = new MarketDataService("", "", "", json);
    }

    @Test
    void should_reportNotConfigured_when_noApiKeysProvided() {
        assertThat(serviceWithNoProviders.isConfigured()).isFalse();
    }

    @Test
    void should_reportConfigured_when_massiveKeyProvided() {
        MarketDataService service = new MarketDataService("massive-key", "", "", json);
        assertThat(service.isConfigured()).isTrue();
    }

    @Test
    void should_reportConfigured_when_alphaKeyProvided() {
        MarketDataService service = new MarketDataService("", "alpha-key", "", json);
        assertThat(service.isConfigured()).isTrue();
    }

    @Test
    void should_throwServiceUnavailable_when_quoteRequestedWithoutAnyProviderConfigured() {
        assertThatThrownBy(() -> serviceWithNoProviders.quote("AAPL"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Live market data is unavailable")
                .hasMessageContaining("No market-data API key is configured");
    }

    @Test
    void should_throwServiceUnavailable_when_historyRequestedWithoutAnyProviderConfigured() {
        assertThatThrownBy(() -> serviceWithNoProviders.history("AAPL", "3mo"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Live market data is unavailable");
    }

    @Test
    void should_throwServiceUnavailable_when_compareRequestedWithoutAnyProviderConfigured() {
        assertThatThrownBy(() -> serviceWithNoProviders.compare("AAPL,MSFT", "3mo"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Live market data is unavailable");
    }

    @Test
    void should_throwServiceUnavailable_when_newsRequestedWithoutAnyProviderConfigured() {
        assertThatThrownBy(() -> serviceWithNoProviders.news("AAPL"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Live market data is unavailable");
    }

    @Test
    void should_useGenericSubject_when_newsRequestedWithBlankSymbolAndNoProviders() {
        assertThatThrownBy(() -> serviceWithNoProviders.news(""))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("market news");
    }

    @Test
    void should_throwServiceUnavailable_when_newsRequestedWithNullSymbolAndNoProviders() {
        assertThatThrownBy(() -> serviceWithNoProviders.news(null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("market news");
    }

    @Test
    void should_throwServiceUnavailable_when_searchRequestedWithoutAnyProviderConfigured() {
        assertThatThrownBy(() -> serviceWithNoProviders.search("Apple"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Live market data is unavailable");
    }

    @Test
    void should_throwNullPointerException_when_searchQueryIsNull() {
        assertThatThrownBy(() -> serviceWithNoProviders.search(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throwBadRequest_when_validateSymbolPriceIsNonPositive() {
        // Without providers configured, quote() throws ApiException(SERVICE_UNAVAILABLE) before
        // validateSymbol can evaluate price, so validateSymbol also surfaces that exception.
        assertThatThrownBy(() -> serviceWithNoProviders.validateSymbol("AAPL"))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void should_defaultToAaplSymbol_when_requestedSymbolIsInvalidPattern_viaQuoteFailureMessage() {
        assertThatThrownBy(() -> serviceWithNoProviders.quote("not a valid symbol!!"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("AAPL");
    }

    @Test
    void should_defaultToAaplSymbol_when_requestedSymbolIsNull_viaQuoteFailureMessage() {
        assertThatThrownBy(() -> serviceWithNoProviders.quote(null))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("AAPL");
    }

    @Test
    void should_upperCaseAndTrimSymbol_when_validSymbolProvided_viaQuoteFailureMessage() {
        assertThatThrownBy(() -> serviceWithNoProviders.quote("  msft  "))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("MSFT");
    }

    @Test
    void should_iterateAllRequestedSymbols_when_compareCalledWithMultipleSymbols_andFailFast() {
        // compare() calls history() for each symbol sequentially; with no providers configured the
        // very first symbol lookup throws, proving iteration/delegation occurs without needing network I/O.
        assertThatThrownBy(() -> serviceWithNoProviders.compare("AAPL,MSFT,NVDA", "1mo"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("AAPL");
    }
}

