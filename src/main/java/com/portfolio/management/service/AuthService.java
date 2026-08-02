package com.portfolio.management.service;

import com.portfolio.management.dto.request.LoginRequest;
import com.portfolio.management.dto.request.RegisterRequest;
import com.portfolio.management.dto.response.JwtResponse;

public interface AuthService {

    /**
     * Register a new user
     *
     * @param registerRequest Registration details
     * @return JWT response a
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     *
     * fter successful registration
     */
    JwtResponse register(RegisterRequest registerRequest);

    /**
     * Authenticate user
     *
     * @param loginRequest Login credentials
     * @return JWT response after successful login
     */
    JwtResponse login(LoginRequest loginRequest);
}