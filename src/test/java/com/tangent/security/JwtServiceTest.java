package com.tangent.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link JwtService}. No Spring context is started; the service is constructed directly.
 */
class JwtServiceTest {

    private static final String SECRET = "TestSecretKeyForTangentThatIsAtLeastThirtyTwoBytesLong";
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 86_400_000L);
    }

    @Test
    void should_createValidToken_when_userIdAndEmailProvided() {
        String token = jwtService.create(42L, "user@tangent.local");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void should_extractUserId_when_tokenIsValid() {
        String token = jwtService.create(42L, "user@tangent.local");

        long userId = jwtService.userId(token);

        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void should_throwException_when_tokenIsMalformed() {
        assertThatThrownBy(() -> jwtService.userId("not-a-valid-jwt"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void should_throwException_when_tokenIsNull() {
        assertThatThrownBy(() -> jwtService.userId(null))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void should_throwException_when_tokenIsBlank() {
        assertThatThrownBy(() -> jwtService.userId(""))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void should_throwException_when_tokenSignedWithDifferentKey() {
        SecretKey otherKey = Keys.hmacShaKeyFor("AnotherSecretKeyThatIsDifferentAndLongEnough".getBytes(StandardCharsets.UTF_8));
        String foreignToken = Jwts.builder()
                .setSubject("1")
                .signWith(otherKey)
                .compact();

        assertThatThrownBy(() -> jwtService.userId(foreignToken))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void should_throwExpiredJwtException_when_tokenHasExpired() {
        JwtService shortLivedService = new JwtService(SECRET, -1000L);
        String token = shortLivedService.create(1L, "user@tangent.local");

        assertThatThrownBy(() -> jwtService.userId(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void should_embedEmailClaim_when_tokenCreated() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = jwtService.create(7L, "someone@tangent.local");

        String email = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().get("email", String.class);

        assertThat(email).isEqualTo("someone@tangent.local");
    }
}

