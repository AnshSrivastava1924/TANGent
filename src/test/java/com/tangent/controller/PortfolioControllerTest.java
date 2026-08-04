package com.tangent.controller;

import com.tangent.dto.ExpenseResponse;
import com.tangent.dto.PortfolioBootstrapResponse;
import com.tangent.dto.UserSummary;
import com.tangent.exception.ApiException;
import com.tangent.exception.GlobalExceptionHandler;
import com.tangent.service.PortfolioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc unit tests for {@link PortfolioController}. The {@code Authentication} principal is
 * supplied directly on the request builder, so no Spring Security filter chain is involved.
 */
@ExtendWith(MockitoExtension.class)
class PortfolioControllerTest {

    private static final long USER_ID = 7L;

    @Mock
    private PortfolioService portfolioService;

    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        PortfolioController controller = new PortfolioController(portfolioService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authentication = new UsernamePasswordAuthenticationToken(
                USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void should_returnOkWithBootstrapData_when_userIsAuthenticated() throws Exception {
        PortfolioBootstrapResponse workspace = new PortfolioBootstrapResponse(
                new UserSummary(USER_ID, "user@tangent.local", "User"), 1L, List.of(), List.of(), List.of());
        when(portfolioService.bootstrap(USER_ID)).thenReturn(workspace);

        mockMvc.perform(get("/api/app/bootstrap").principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.portfolioId").value(1))
                .andExpect(jsonPath("$.data.user.email").value("user@tangent.local"));
    }

    @Test
    void should_returnNotFound_when_bootstrapUserDoesNotExist() throws Exception {
        when(portfolioService.bootstrap(USER_ID))
                .thenThrow(new ApiException(HttpStatus.NOT_FOUND, "User workspace not found"));

        mockMvc.perform(get("/api/app/bootstrap").principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User workspace not found"));
    }

    @Test
    void should_returnNoContent_when_updateAssetSucceeds() throws Exception {
        String body = """
                {"value":50000,"income":1800}
                """;

        mockMvc.perform(put("/api/app/assets/1").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        verify(portfolioService).updateAsset(USER_ID, 1L, new BigDecimal("50000"), new BigDecimal("1800"));
    }

    @Test
    void should_returnNotFound_when_updateAssetDoesNotExist() throws Exception {
        doThrow(new ApiException(HttpStatus.NOT_FOUND, "Asset not found"))
                .when(portfolioService).updateAsset(eq(USER_ID), eq(99L), any(), any());
        String body = """
                {"value":50000,"income":1800}
                """;

        mockMvc.perform(put("/api/app/assets/99").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_returnBadRequest_when_updateAssetValueIsMissing() throws Exception {
        String body = """
                {"income":1800}
                """;

        mockMvc.perform(put("/api/app/assets/1").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.value").exists());
    }

    @Test
    void should_returnBadRequest_when_updateAssetValueIsNegative() throws Exception {
        String body = """
                {"value":-50,"income":1800}
                """;

        mockMvc.perform(put("/api/app/assets/1").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.value").exists());
    }

    @Test
    void should_returnCreatedWithExpense_when_addExpenseSucceeds() throws Exception {
        ExpenseResponse response = new ExpenseResponse(5L, LocalDate.of(2026, 8, 2), "Groceries", "Food", new BigDecimal("85"));
        when(portfolioService.addExpense(eq(USER_ID), any())).thenReturn(response);
        String body = """
                {"name":"Groceries","category":"Food","amount":85,"date":"2026-08-02"}
                """;

        mockMvc.perform(post("/api/app/expenses").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Expense created"))
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("Groceries"));
    }

    @Test
    void should_returnBadRequest_when_addExpenseCategoryIsUnknown() throws Exception {
        when(portfolioService.addExpense(eq(USER_ID), any()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "Unknown expense category"));
        String body = """
                {"name":"Groceries","category":"Unknown","amount":85,"date":"2026-08-02"}
                """;

        mockMvc.perform(post("/api/app/expenses").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown expense category"));
    }

    @Test
    void should_returnBadRequest_when_addExpenseNameIsBlank() throws Exception {
        String body = """
                {"name":"","category":"Food","amount":85,"date":"2026-08-02"}
                """;

        mockMvc.perform(post("/api/app/expenses").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void should_returnBadRequest_when_addExpenseAmountIsZeroOrNegative() throws Exception {
        String body = """
                {"name":"Groceries","category":"Food","amount":0,"date":"2026-08-02"}
                """;

        mockMvc.perform(post("/api/app/expenses").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void should_returnNoContent_when_updateExpenseSucceeds() throws Exception {
        String body = """
                {"amount":90}
                """;

        mockMvc.perform(put("/api/app/expenses/3").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNoContent());

        verify(portfolioService).updateExpense(USER_ID, 3L, new BigDecimal("90"));
    }

    @Test
    void should_returnNotFound_when_updateExpenseDoesNotExist() throws Exception {
        doThrow(new ApiException(HttpStatus.NOT_FOUND, "Expense not found"))
                .when(portfolioService).updateExpense(eq(USER_ID), eq(999L), any());
        String body = """
                {"amount":90}
                """;

        mockMvc.perform(put("/api/app/expenses/999").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void should_returnCreatedWithSymbol_when_addWatchlistSucceeds() throws Exception {
        when(portfolioService.addWatchSymbol(USER_ID, "AAPL")).thenReturn("AAPL");
        String body = """
                {"symbol":"AAPL"}
                """;

        mockMvc.perform(post("/api/app/watchlist").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.symbol").value("AAPL"));
    }

    @Test
    void should_returnBadRequest_when_watchlistSymbolIsBlank() throws Exception {
        String body = """
                {"symbol":""}
                """;

        mockMvc.perform(post("/api/app/watchlist").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.symbol").exists());
    }

    @Test
    void should_returnBadRequest_when_addWatchlistSymbolIsInvalid() throws Exception {
        when(portfolioService.addWatchSymbol(eq(USER_ID), anyString()))
                .thenThrow(new ApiException(HttpStatus.BAD_REQUEST, "Invalid symbol"));
        String body = """
                {"symbol":"bad symbol!"}
                """;

        mockMvc.perform(post("/api/app/watchlist").principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid symbol"));
    }

    @Test
    void should_returnNoContent_when_removeWatchlistSucceeds() throws Exception {
        mockMvc.perform(delete("/api/app/watchlist/AAPL").principal(authentication))
                .andExpect(status().isNoContent());

        verify(portfolioService).removeWatchSymbol(USER_ID, "AAPL");
    }

    @Test
    void should_returnInternalServerError_when_removeWatchlistThrowsUnexpectedException() throws Exception {
        org.mockito.Mockito.doThrow(new RuntimeException("db failure"))
                .when(portfolioService).removeWatchSymbol(anyLong(), anyString());

        mockMvc.perform(delete("/api/app/watchlist/AAPL").principal(authentication))
                .andExpect(status().isInternalServerError());
    }
}

