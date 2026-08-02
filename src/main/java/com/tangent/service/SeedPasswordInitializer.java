package com.tangent.service;

import com.tangent.repository.AuthRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class SeedPasswordInitializer implements ApplicationRunner {

    private final AuthRepository users;
    private final PasswordEncoder encoder;

    public SeedPasswordInitializer(AuthRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        users.replaceSeedPassword(encoder.encode("training123"));
    }
}
