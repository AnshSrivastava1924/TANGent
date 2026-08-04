package com.tangent.service;

import com.tangent.dto.AuthRequest;
import com.tangent.dto.AuthResponse;
import com.tangent.dto.UserResponse;
import com.tangent.exception.ApiException;
import com.tangent.repository.AuthRepository;
import com.tangent.repository.PortfolioRepository;
import com.tangent.repository.UserAccount;
import com.tangent.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final AuthRepository users;
    private final PortfolioRepository portfolios;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(AuthRepository users, PortfolioRepository portfolios,
                       PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.portfolios = portfolios;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        return "signup".equalsIgnoreCase(request.mode())
                ? register(email, request.password(), request.fullName())
                : login(email, request.password());
    }

    private AuthResponse register(String email, String password, String requestedName) {
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account already exists for this email");
        }
        String fullName = requestedName == null || requestedName.isBlank()
                ? displayName(email) : requestedName.trim();
        long userId = users.createUser(email, passwordEncoder.encode(password), fullName);
        portfolios.getOrCreatePortfolio(userId);
        portfolios.createDefaultExpenseCategories(userId);
        return response(new UserAccount(userId, email, "", fullName));
    }

    private AuthResponse login(String email, String password) {
        UserAccount user = users.findActiveByEmail(email)
                .filter(account -> passwordEncoder.matches(password, account.passwordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        return response(user);
    }

    private AuthResponse response(UserAccount user) {
        return new AuthResponse(jwtService.create(user.id(), user.email()), "Bearer",
                new UserResponse(user.id(), user.email(), user.fullName()));
    }

    private String displayName(String email) {
        String local = email.substring(0, email.indexOf('@'));
        return local.isBlank() ? "TANGent User"
                : Character.toUpperCase(local.charAt(0)) + local.substring(1);
    }
}
