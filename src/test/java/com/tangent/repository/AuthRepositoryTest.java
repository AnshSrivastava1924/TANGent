package com.tangent.repository;

import com.tangent.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuthRepository} with a mocked {@link JdbcTemplate}. No real database
 * connection is used.
 */
@ExtendWith(MockitoExtension.class)
class AuthRepositoryTest {

    @Mock
    private JdbcTemplate jdbc;

    private AuthRepository repository;

    @Test
    void should_returnTrue_when_emailExistsCountIsPositive() {
        repository = new AuthRepository(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("user@tangent.local"))).thenReturn(1);

        boolean exists = repository.existsByEmail("user@tangent.local");

        assertThat(exists).isTrue();
    }

    @Test
    void should_returnFalse_when_emailExistsCountIsZero() {
        repository = new AuthRepository(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("new@tangent.local"))).thenReturn(0);

        boolean exists = repository.existsByEmail("new@tangent.local");

        assertThat(exists).isFalse();
    }

    @Test
    void should_returnFalse_when_emailExistsCountIsNull() {
        repository = new AuthRepository(jdbc);
        when(jdbc.queryForObject(anyString(), eq(Integer.class), eq("new@tangent.local"))).thenReturn(null);

        boolean exists = repository.existsByEmail("new@tangent.local");

        assertThat(exists).isFalse();
    }

    @Test
    void should_returnGeneratedId_when_createUserSucceeds() throws SQLException {
        repository = new AuthRepository(jdbc);
        stubGeneratedKey(100L);

        long id = repository.createUser("user@tangent.local", "hash", "User Name");

        assertThat(id).isEqualTo(100L);
    }

    @Test
    void should_throwInternalServerError_when_createUserGeneratesNoKey() {
        repository = new AuthRepository(jdbc);
        // update(...) called but keyHolder left empty -> getKey() returns null

        assertThatThrownBy(() -> repository.createUser("user@tangent.local", "hash", "User Name"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unable to create user");
    }

    @Test
    void should_returnUserAccount_when_activeEmailFound() throws SQLException {
        repository = new AuthRepository(jdbc);
        UserAccount expected = new UserAccount(1L, "user@tangent.local", "hash", "User Name");
        stubRowMapperQuery(List.of(expected));

        Optional<UserAccount> result = repository.findActiveByEmail("user@tangent.local");

        assertThat(result).isPresent().contains(expected);
    }

    @Test
    void should_returnEmptyOptional_when_activeEmailNotFound() {
        repository = new AuthRepository(jdbc);
        stubRowMapperQuery(List.of());

        Optional<UserAccount> result = repository.findActiveByEmail("missing@tangent.local");

        assertThat(result).isEmpty();
    }

    @Test
    void should_delegateToJdbcUpdate_when_replacingSeedPassword() {
        repository = new AuthRepository(jdbc);

        repository.replaceSeedPassword("new-hash");

        verify(jdbc).update(
                eq("UPDATE users SET password_hash = ? WHERE password_hash = '{seed}'"),
                eq("new-hash"));
    }

    @SuppressWarnings("unchecked")
    private void stubRowMapperQuery(List<UserAccount> results) {
        when(jdbc.query(anyString(), any(RowMapper.class), any(Object[].class))).thenReturn(results);
    }

    @SuppressWarnings("unchecked")
    private void stubGeneratedKey(long generatedKey) {
        when(jdbc.update(any(PreparedStatementCreator.class), any(KeyHolder.class))).thenAnswer(invocation -> {
            KeyHolder holder = invocation.getArgument(1);
            holder.getKeyList().add(java.util.Map.of("user_id", generatedKey));
            return 1;
        });
    }
}


