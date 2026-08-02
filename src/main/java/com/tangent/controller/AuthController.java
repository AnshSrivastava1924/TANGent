package com.tangent.controller;

import com.tangent.dto.AuthRequest;
import com.tangent.dto.AuthResponse;
import com.tangent.service.AuthService;
import com.tangent.wrapper.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    @SecurityRequirements
    @Operation(summary = "Log in or create an account", security = {})
    public ApiResponse<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        return ApiResponse.success(authService.authenticate(request));
    }
}
