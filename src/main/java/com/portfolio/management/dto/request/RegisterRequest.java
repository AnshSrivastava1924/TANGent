package com.portfolio.management.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 160, message = "Full name cannot exceed 160 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters")
    private String password;

    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @NotBlank(message = "Risk profile is required")
    @Pattern(
            regexp = "CONSERVATIVE|MODERATE|AGGRESSIVE",
            message = "Risk profile must be CONSERVATIVE, MODERATE or AGGRESSIVE"
    )
    private String riskProfile;

    @NotBlank(message = "Base currency is required")
    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    private String baseCurrency;
}