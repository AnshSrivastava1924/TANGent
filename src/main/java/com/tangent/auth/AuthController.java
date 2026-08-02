package com.tangent.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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
    public ResponseEntity<Map<String, Object>> authenticate(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(
                request.mode(), request.email(), request.password(), request.fullName()));
    }

    public record AuthRequest(
            @Schema(example = "login", allowableValues = {"login", "signup"})
            @Pattern(regexp = "login|signup") String mode,
            @Schema(example = "student@tangent.local")
            @NotBlank @Email String email,
            @Schema(example = "training123")
            @NotBlank @Size(min = 8, max = 72) String password,
            @Schema(example = "Anita Sharma") String fullName
    ) {
        public AuthRequest {
            if (mode == null || mode.isBlank()) mode = "login";
        }
    }
}
