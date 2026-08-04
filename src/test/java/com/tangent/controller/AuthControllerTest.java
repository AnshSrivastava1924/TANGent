package com.tangent.controller;

import com.tangent.dto.AuthResponse;
import com.tangent.dto.UserResponse;
import com.tangent.exception.ApiException;
import com.tangent.exception.GlobalExceptionHandler;
import com.tangent.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc unit tests for {@link AuthController}. No Spring application context is started;
 * the controller is wired manually with a mocked {@link AuthService}.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController(authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void should_returnOkWithToken_when_loginRequestIsValid() throws Exception {
        AuthResponse response = new AuthResponse("jwt-token", "Bearer", new UserResponse(1L, "user@tangent.local", "User"));
        when(authService.authenticate(any())).thenReturn(response);

        String body = """
                {"mode":"login","email":"user@tangent.local","password":"training123"}
                """;

        mockMvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").value("jwt-token"))
                .andExpect(jsonPath("$.data.user.email").value("user@tangent.local"));

        verify(authService).authenticate(any());
    }

    @Test
    void should_returnOkWithToken_when_signupRequestIsValid() throws Exception {
        AuthResponse response = new AuthResponse("jwt-token", "Bearer", new UserResponse(2L, "new@tangent.local", "New User"));
        when(authService.authenticate(any())).thenReturn(response);

        String body = """
                {"mode":"signup","email":"new@tangent.local","password":"training123","fullName":"New User"}
                """;

        mockMvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.fullName").value("New User"));
    }

    @Test
    void should_returnBadRequest_when_emailIsBlank() throws Exception {
        String body = """
                {"mode":"login","email":"","password":"training123"}
                """;

        mockMvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void should_returnBadRequest_when_emailIsNotValidFormat() throws Exception {
        String body = """
                {"mode":"login","email":"not-an-email","password":"training123"}
                """;

        mockMvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void should_returnBadRequest_when_passwordIsTooShort() throws Exception {
        String body = """
                {"mode":"login","email":"user@tangent.local","password":"short"}
                """;

        mockMvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    void should_returnBadRequest_when_modeIsInvalidValue() throws Exception {
        String body = """
                {"mode":"invalid-mode","email":"user@tangent.local","password":"training123"}
                """;

        mockMvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.mode").exists());
    }

    @Test
    void should_returnConflict_when_serviceThrowsApiExceptionForDuplicateEmail() throws Exception {
        when(authService.authenticate(any()))
                .thenThrow(new ApiException(HttpStatus.CONFLICT, "An account already exists for this email"));

        String body = """
                {"mode":"signup","email":"existing@tangent.local","password":"training123"}
                """;

        mockMvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account already exists for this email"));
    }

    @Test
    void should_returnUnauthorized_when_serviceThrowsApiExceptionForBadCredentials() throws Exception {
        when(authService.authenticate(any()))
                .thenThrow(new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        String body = """
                {"mode":"login","email":"user@tangent.local","password":"training123"}
                """;

        mockMvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void should_returnInternalServerError_when_requestBodyIsMissing() throws Exception {
        // NOTE: GlobalExceptionHandler has no dedicated handler for HttpMessageNotReadableException,
        // so a missing/unparseable body currently falls through to the generic 500 handler instead
        // of a 400. This documents the current behavior; consider adding a handler for
        // HttpMessageNotReadableException in GlobalExceptionHandler if a 400 response is desired.
        mockMvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void should_returnInternalServerError_when_unexpectedExceptionOccurs() throws Exception {
        when(authService.authenticate(any())).thenThrow(new RuntimeException("db down"));

        String body = """
                {"mode":"login","email":"user@tangent.local","password":"training123"}
                """;

        mockMvc.perform(post("/api/auth").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected server error"));
    }
}

