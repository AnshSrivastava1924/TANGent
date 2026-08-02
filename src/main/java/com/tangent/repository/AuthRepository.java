package com.tangent.repository;

import com.tangent.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.Optional;

@Repository
public class AuthRepository {

    private final JdbcTemplate jdbc;

    public AuthRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean existsByEmail(String email) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        return count != null && count > 0;
    }

    public long createUser(String email, String passwordHash, String fullName) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO users
                        (email, password_hash, full_name, risk_profile, base_currency, is_active, created_at, updated_at)
                    VALUES (?, ?, ?, 'moderate', 'USD', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, new String[]{"user_id"});
            statement.setString(1, email);
            statement.setString(2, passwordHash);
            statement.setString(3, fullName);
            return statement;
        }, keys);
        Number key = keys.getKey();
        if (key == null) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create user");
        }
        return key.longValue();
    }

    public Optional<UserAccount> findActiveByEmail(String email) {
        return jdbc.query("""
                        SELECT user_id, email, password_hash, full_name
                        FROM users WHERE email = ? AND is_active = TRUE
                        """,
                (rs, row) -> new UserAccount(rs.getLong("user_id"), rs.getString("email"),
                        rs.getString("password_hash"), rs.getString("full_name")), email)
                .stream().findFirst();
    }

    public void replaceSeedPassword(String passwordHash) {
        jdbc.update("UPDATE users SET password_hash = ? WHERE password_hash = '{seed}'", passwordHash);
    }
}
