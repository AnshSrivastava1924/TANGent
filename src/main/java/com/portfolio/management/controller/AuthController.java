package com.portfolio.management.controller;

import com.portfolio.management.dto.request.LoginRequest;
import com.portfolio.management.dto.request.RegisterRequest;
import com.portfolio.management.dto.response.JwtResponse;
import com.portfolio.management.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<JwtResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest) {

        JwtResponse response = authService.register(registerRequest);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest loginRequest) {

        JwtResponse response = authService.login(loginRequest);

        return ResponseEntity.ok(response);
    }
}