package com.tangent.service;

import com.tangent.dto.AuthRequest;
import com.tangent.dto.AuthResponse;
import com.tangent.exception.ApiException;
import com.tangent.repository.AuthRepository;
import com.tangent.repository.PortfolioRepository;
import com.tangent.repository.UserAccount;
import com.tangent.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthService}. All collaborators are mocked; no Spring context is started.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthRepository users;

    @Mock
    private PortfolioRepository portfolios;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private AuthRequest signupRequest;
    private AuthRequest loginRequest;

    @BeforeEach
    void setUp() {
        signupRequest = new AuthRequest("signup", "Test@Tangent.local", "Password123", "Test User");
        loginRequest = new AuthRequest("login", "Test@Tangent.local", "Password123", null);
    }

    @Test
    void should_registerNewUser_when_signupModeAndEmailNotTaken() {
        when(users.existsByEmail("test@tangent.local")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hashed-password");
        when(users.createUser("test@tangent.local", "hashed-password", "Test User")).thenReturn(1L);
        when(jwtService.create(1L, "test@tangent.local")).thenReturn("jwt-token");

        AuthResponse response = authService.authenticate(signupRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.type()).isEqualTo("Bearer");
        assertThat(response.user().id()).isEqualTo(1L);
        assertThat(response.user().email()).isEqualTo("test@tangent.local");
        assertThat(response.user().fullName()).isEqualTo("Test User");
        verify(portfolios, times(1)).createStarterWorkspace(1L);
        verify(users, times(1)).existsByEmail("test@tangent.local");
        verify(users, times(1)).createUser("test@tangent.local", "hashed-password", "Test User");
    }

    @Test
    void should_deriveDisplayName_when_signupRequestedNameIsBlank() {
        AuthRequest blankNameRequest = new AuthRequest("signup", "jane@tangent.local", "Password123", "  ");
        when(users.existsByEmail("jane@tangent.local")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(users.createUser(eq("jane@tangent.local"), eq("hashed"), eq("Jane"))).thenReturn(2L);
        when(jwtService.create(anyLong(), anyString())).thenReturn("token");

        AuthResponse response = authService.authenticate(blankNameRequest);

        assertThat(response.user().fullName()).isEqualTo("Jane");
        verify(users).createUser("jane@tangent.local", "hashed", "Jane");
    }

    @Test
    void should_useFallbackDisplayName_when_signupEmailLocalPartIsBlank() {
        AuthRequest edgeCaseRequest = new AuthRequest("signup", "@tangent.local", "Password123", null);
        when(users.existsByEmail("@tangent.local")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(users.createUser(eq("@tangent.local"), eq("hashed"), eq("TANGent User"))).thenReturn(3L);
        when(jwtService.create(anyLong(), anyString())).thenReturn("token");

        AuthResponse response = authService.authenticate(edgeCaseRequest);

        assertThat(response.user().fullName()).isEqualTo("TANGent User");
    }

    @Test
    void should_throwConflict_when_signupEmailAlreadyExists() {
        when(users.existsByEmail("test@tangent.local")).thenReturn(true);

        assertThatThrownBy(() -> authService.authenticate(signupRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("already exists")
                .satisfies(exception -> assertThat(((ApiException) exception).status()).isEqualTo(HttpStatus.CONFLICT));

        verify(portfolios, never()).createStarterWorkspace(anyLong());
        verify(users, never()).createUser(anyString(), anyString(), anyString());
    }

    @Test
    void should_loginSuccessfully_when_credentialsAreValid() {
        UserAccount account = new UserAccount(5L, "test@tangent.local", "hashed-password", "Test User");
        when(users.findActiveByEmail("test@tangent.local")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Password123", "hashed-password")).thenReturn(true);
        when(jwtService.create(5L, "test@tangent.local")).thenReturn("jwt-token");

        AuthResponse response = authService.authenticate(loginRequest);

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.user().id()).isEqualTo(5L);
        verify(jwtService).create(5L, "test@tangent.local");
    }

    @Test
    void should_throwUnauthorized_when_loginEmailNotFound() {
        when(users.findActiveByEmail("test@tangent.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(loginRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid email or password")
                .satisfies(exception -> assertThat(((ApiException) exception).status()).isEqualTo(HttpStatus.UNAUTHORIZED));

        verify(jwtService, never()).create(anyLong(), anyString());
    }

    @Test
    void should_throwUnauthorized_when_loginPasswordDoesNotMatch() {
        UserAccount account = new UserAccount(5L, "test@tangent.local", "hashed-password", "Test User");
        when(users.findActiveByEmail("test@tangent.local")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Password123", "hashed-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate(loginRequest))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid email or password");

        verify(jwtService, never()).create(anyLong(), anyString());
    }

    @Test
    void should_normalizeEmail_when_emailHasMixedCaseAndWhitespace() {
        AuthRequest request = new AuthRequest("login", "  Test@Tangent.LOCAL  ", "Password123", null);
        when(users.findActiveByEmail("test@tangent.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(request)).isInstanceOf(ApiException.class);

        verify(users).findActiveByEmail("test@tangent.local");
    }

    @Test
    void should_treatUnknownMode_asLogin_when_modeIsNeitherLoginNorSignup() {
        AuthRequest request = new AuthRequest("bogus", "test@tangent.local", "Password123", null);
        when(users.findActiveByEmail("test@tangent.local")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate(request)).isInstanceOf(ApiException.class);

        verify(users, never()).createUser(anyString(), anyString(), anyString());
        verify(users).findActiveByEmail("test@tangent.local");
    }

    @Test
    void should_throwNullPointerException_when_requestIsNull() {
        assertThatThrownBy(() -> authService.authenticate(null))
                .isInstanceOf(NullPointerException.class);
    }
}

