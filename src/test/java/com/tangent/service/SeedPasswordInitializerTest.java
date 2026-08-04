package com.tangent.service;

import com.tangent.repository.AuthRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SeedPasswordInitializer}.
 */
@ExtendWith(MockitoExtension.class)
class SeedPasswordInitializerTest {

    @Mock
    private AuthRepository users;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private SeedPasswordInitializer initializer;

    @Test
    void should_replaceSeedPassword_when_applicationStarts() {
        when(encoder.encode("training123")).thenReturn("encoded-seed-password");
        ApplicationArguments args = mock(ApplicationArguments.class);

        initializer.run(args);

        verify(users).replaceSeedPassword("encoded-seed-password");
    }

    @Test
    void should_passEncodedValue_notPlainText_toRepository() {
        when(encoder.encode("training123")).thenReturn("$2a$10$hashvalue");
        ApplicationArguments args = mock(ApplicationArguments.class);

        initializer.run(args);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(users).replaceSeedPassword(captor.capture());
        assertThat(captor.getValue()).isEqualTo("$2a$10$hashvalue").isNotEqualTo("training123");
    }

    @Test
    void should_handleNullApplicationArguments_gracefully() {
        when(encoder.encode("training123")).thenReturn("encoded");

        initializer.run(null);

        verify(users).replaceSeedPassword("encoded");
    }
}

