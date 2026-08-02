package com.tangent;

import com.tangent.dto.AuthRequest;
import com.tangent.dto.AuthResponse;
import com.tangent.dto.PortfolioBootstrapResponse;
import com.tangent.service.AuthService;
import com.tangent.service.PortfolioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:tangent_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/dev-schema.sql",
        "jwt.secret=TestSecretKeyForTangentThatIsAtLeastThirtyTwoBytesLong",
        "market.massive.api-key=",
        "market.alpha-vantage.api-key="
})
@Transactional
class TangentApplicationTests {

    @Autowired
    private AuthService authService;

    @Autowired
    private PortfolioService portfolioService;

    @Test
    void contextLoads() {
    }

    @Test
    void userCanRegisterLoginAndManageWatchlist() {
        AuthResponse registration = authService.authenticate(
                new AuthRequest("signup", "test@tangent.local", "Password123", "Test User"));
        AuthResponse login = authService.authenticate(
                new AuthRequest("login", "test@tangent.local", "Password123", null));

        long userId = registration.user().id();
        PortfolioBootstrapResponse workspace = portfolioService.bootstrap(userId);
        long assetCount = workspace.portfolioClasses().stream()
                .mapToLong(assetClass -> assetClass.items().size())
                .sum();
        portfolioService.addWatchSymbol(userId, "IBM");
        var watchlist = portfolioService.bootstrap(userId).watchlist();

        assertThat(registration.token()).isNotBlank();
        assertThat(login.token()).isNotBlank();
        assertThat(workspace.portfolioClasses()).isNotEmpty();
        assertThat(assetCount).isEqualTo(10);
        assertThat(watchlist).contains("IBM");
    }
}
