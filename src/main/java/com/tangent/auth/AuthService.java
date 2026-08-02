package com.tangent.auth;

import com.tangent.api.ApiException;
import com.tangent.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(JdbcTemplate jdbc, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public Map<String, Object> authenticate(String mode, String email, String password, String fullName) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        return "signup".equalsIgnoreCase(mode)
                ? register(normalizedEmail, password, fullName)
                : login(normalizedEmail, password);
    }

    private Map<String, Object> register(String email, String password, String fullName) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, email);
        if (count != null && count > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "An account already exists for this email");
        }

        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO users
                        (email, password_hash, full_name, risk_profile, base_currency, is_active, created_at, updated_at)
                    VALUES (?, ?, ?, 'moderate', 'USD', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, new String[]{"user_id"});
            statement.setString(1, email);
            statement.setString(2, passwordEncoder.encode(password));
            statement.setString(3, fullName == null || fullName.isBlank() ? displayName(email) : fullName.trim());
            return statement;
        }, keys);
        long userId = requiredKey(keys);
        createUserWorkspace(userId);
        return response(userId, email, fullName == null || fullName.isBlank() ? displayName(email) : fullName.trim());
    }

    private Map<String, Object> login(String email, String password) {
        var users = jdbc.query("""
                        SELECT user_id, email, password_hash, full_name
                        FROM users WHERE email = ? AND is_active = TRUE
                        """,
                (rs, row) -> Map.<String, Object>of(
                        "id", rs.getLong("user_id"),
                        "email", rs.getString("email"),
                        "password", rs.getString("password_hash"),
                        "fullName", rs.getString("full_name")
                ), email);
        if (users.isEmpty() || !passwordEncoder.matches(password, (String) users.get(0).get("password"))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        Map<String, Object> user = users.get(0);
        return response((Long) user.get("id"), (String) user.get("email"), (String) user.get("fullName"));
    }

    private Map<String, Object> response(long id, String email, String fullName) {
        return Map.of(
                "token", jwtService.create(id, email),
                "type", "Bearer",
                "user", Map.of("id", id, "email", email, "fullName", fullName)
        );
    }

    private void createUserWorkspace(long userId) {
        jdbc.update("INSERT INTO portfolios (user_id, portfolio_name, goal_description) VALUES (?, ?, ?)",
                userId, "My Portfolio", "Build long-term financial security");
        jdbc.update("INSERT INTO watchlists (user_id, watchlist_name) VALUES (?, 'Default Watchlist')", userId);
        for (String category : new String[]{"Food", "Health", "Housing", "Utilities", "Transport", "Family", "Leisure"}) {
            jdbc.update("INSERT INTO buddy_categories (user_id, category_name, monthly_budget, color_hex) VALUES (?, ?, 0, '#007AFF')",
                    userId, category);
        }
    }

    private long requiredKey(GeneratedKeyHolder keys) {
        Number key = keys.getKey();
        if (key == null) throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create user");
        return key.longValue();
    }

    private String displayName(String email) {
        String local = email.substring(0, email.indexOf('@'));
        return local.isBlank() ? "TANGent User" : Character.toUpperCase(local.charAt(0)) + local.substring(1);
    }
}
