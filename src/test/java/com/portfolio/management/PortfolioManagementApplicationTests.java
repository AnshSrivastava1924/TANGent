package com.portfolio.management;

import com.tangent.app.AppDataService;
import com.tangent.auth.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

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
class PortfolioManagementApplicationTests {

	@Autowired
	private AuthService authService;

	@Autowired
	private AppDataService appDataService;

	@Test
	void contextLoads() {
	}

	@Test
	void userCanRegisterAndLogin() {
		Map<String, Object> registration = authService.authenticate(
				"signup", "test@tangent.local", "Password123", "Test User");
		Map<String, Object> login = authService.authenticate(
				"login", "test@tangent.local", "Password123", null);

		@SuppressWarnings("unchecked")
		Map<String, Object> user = (Map<String, Object>) registration.get("user");
		assertThat(registration.get("token")).asString().isNotBlank();
		assertThat(login.get("token")).asString().isNotBlank();
		assertThat(appDataService.bootstrap(((Number) user.get("id")).longValue()))
				.containsKeys("portfolioClasses", "expenses", "watchlist");
	}

}
