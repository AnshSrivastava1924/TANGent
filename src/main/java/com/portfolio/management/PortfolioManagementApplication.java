package com.portfolio.management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(
		scanBasePackages = "com.tangent",
		exclude = UserDetailsServiceAutoConfiguration.class
)
public class PortfolioManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(PortfolioManagementApplication.class, args);
	}

}
