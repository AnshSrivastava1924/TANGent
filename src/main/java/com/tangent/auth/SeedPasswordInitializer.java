package com.tangent.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SeedPasswordInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    public SeedPasswordInitializer(JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        jdbc.update("UPDATE users SET password_hash = ? WHERE password_hash = '{seed}'",
                encoder.encode("training123"));
    }
}
