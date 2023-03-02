package io.kokuwa.keycloak.metrics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import io.kokuwa.keycloak.metrics.junit.KeycloakClient;
import io.kokuwa.keycloak.metrics.junit.KeycloakExtension;
import io.kokuwa.keycloak.metrics.prometheus.Prometheus;

@ExtendWith(KeycloakExtension.class)
public class KeycloakIT {

	@DisplayName("login and attempts")
	@Test
	void loginAndAttempts(KeycloakClient keycloak, Prometheus prometheus) {

		var realmName1 = UUID.randomUUID().toString();
		var username1 = UUID.randomUUID().toString();
		var password1 = UUID.randomUUID().toString();
		var realmName2 = UUID.randomUUID().toString();
		var username2 = UUID.randomUUID().toString();
		var password2 = UUID.randomUUID().toString();
		var realmId1 = keycloak.createRealm(realmName1);
		var realmId2 = keycloak.createRealm(realmName2);
		keycloak.createUser(realmName1, username1, password1);
		keycloak.createUser(realmName2, username2, password2);

		prometheus.scrap();
		var loginAttemptsBefore = prometheus.loginAttempts();
		var loginAttemptsBefore1 = prometheus.loginAttempts(realmId1);
		var loginAttemptsBefore2 = prometheus.loginAttempts(realmId2);
		var loginSuccessBefore = prometheus.loginSuccess();
		var loginSuccessBefore1 = prometheus.loginSuccess(realmId1);
		var loginSuccessBefore2 = prometheus.loginSuccess(realmId2);
		var loginFailureBefore = prometheus.loginFailure();
		var loginFailureBefore1 = prometheus.loginFailure(realmId1);
		var loginFailureBefore2 = prometheus.loginFailure(realmId2);

		assertTrue(keycloak.login(realmName1, username1, password1));
		assertTrue(keycloak.login(realmName1, username1, password1));
		assertTrue(keycloak.login(realmName2, username2, password2));
		assertFalse(keycloak.login(realmName2, username2, "nope"));

		prometheus.scrap();
		var loginAttemptsAfter = prometheus.loginAttempts();
		var loginAttemptsAfter1 = prometheus.loginAttempts(realmId1);
		var loginAttemptsAfter2 = prometheus.loginAttempts(realmId2);
		var loginSuccessAfter = prometheus.loginSuccess();
		var loginSuccessAfter1 = prometheus.loginSuccess(realmId1);
		var loginSuccessAfter2 = prometheus.loginSuccess(realmId2);
		var loginFailureAfter = prometheus.loginFailure();
		var loginFailureAfter1 = prometheus.loginFailure(realmId1);
		var loginFailureAfter2 = prometheus.loginFailure(realmId2);

		assertAll("promethus",
				() -> assertEquals(loginAttemptsBefore + 4, loginAttemptsAfter, "login attempts total"),
				() -> assertEquals(loginAttemptsBefore1 + 2, loginAttemptsAfter1, "login attempts #1"),
				() -> assertEquals(loginAttemptsBefore2 + 2, loginAttemptsAfter2, "login attempts #2"),
				() -> assertEquals(loginSuccessBefore + 3, loginSuccessAfter, "login success total"),
				() -> assertEquals(loginSuccessBefore1 + 2, loginSuccessAfter1, "login success #1"),
				() -> assertEquals(loginSuccessBefore2 + 1, loginSuccessAfter2, "login success #2"),
				() -> assertEquals(loginFailureBefore + 1, loginFailureAfter, "login failure total"),
				() -> assertEquals(loginFailureBefore1 + 0, loginFailureAfter1, "login failure #1"),
				() -> assertEquals(loginFailureBefore2 + 1, loginFailureAfter2, "login failure #2"));
	}
}
