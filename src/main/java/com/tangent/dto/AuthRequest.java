package com.tangent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
