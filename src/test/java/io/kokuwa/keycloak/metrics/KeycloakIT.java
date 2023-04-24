package io.kokuwa.keycloak.metrics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.events.EventType;

import io.kokuwa.keycloak.metrics.junit.KeycloakClient;
import io.kokuwa.keycloak.metrics.junit.KeycloakExtension;
import io.kokuwa.keycloak.metrics.junit.Prometheus;

/**
 * Integration tests with Keycloak.
 *
 * @author Stephan Schnabel
 */
@ExtendWith(KeycloakExtension.class)
public class KeycloakIT {

	@DisplayName("login and attempts")
	@Test
	void loginAndAttempts(KeycloakClient keycloak, Prometheus prometheus) {

		var clientId1 = UUID.randomUUID().toString();
		var realmName1 = UUID.randomUUID().toString();
		var username1 = UUID.randomUUID().toString();
		var password1 = UUID.randomUUID().toString();
		keycloak.createRealm(realmName1);
		keycloak.createClient(realmName1, clientId1);
		keycloak.createUser(realmName1, username1, password1);

		var clientId2 = UUID.randomUUID().toString();
		var realmName2 = UUID.randomUUID().toString();
		var username2 = UUID.randomUUID().toString();
		var password2 = UUID.randomUUID().toString();
		keycloak.createRealm(realmName2);
		keycloak.createClient(realmName2, clientId2);
		keycloak.createUser(realmName2, username2, password2);

		prometheus.scrap();
		var loginBefore = prometheus.userEvent(EventType.LOGIN);
		var loginBefore1 = prometheus.userEvent(EventType.LOGIN, realmName1, clientId1);
		var loginBefore2 = prometheus.userEvent(EventType.LOGIN, realmName2, clientId2);
		var loginErrorBefore = prometheus.userEvent(EventType.LOGIN_ERROR);
		var loginErrorBefore1 = prometheus.userEvent(EventType.LOGIN_ERROR, realmName1, clientId1);
		var loginErrorBefore2 = prometheus.userEvent(EventType.LOGIN_ERROR, realmName2, clientId2);

		assertTrue(keycloak.login(clientId1, realmName1, username1, password1));
		assertTrue(keycloak.login(clientId1, realmName1, username1, password1));
		assertTrue(keycloak.login(clientId2, realmName2, username2, password2));
		assertFalse(keycloak.login(clientId2, realmName2, username2, "nope"));

		prometheus.scrap();
		var loginAfter = prometheus.userEvent(EventType.LOGIN);
		var loginAfter1 = prometheus.userEvent(EventType.LOGIN, realmName1, clientId1);
		var loginAfter2 = prometheus.userEvent(EventType.LOGIN, realmName2, clientId2);
		var loginErrorAfter = prometheus.userEvent(EventType.LOGIN_ERROR);
		var loginErrorAfter1 = prometheus.userEvent(EventType.LOGIN_ERROR, realmName1, clientId1);
		var loginErrorAfter2 = prometheus.userEvent(EventType.LOGIN_ERROR, realmName2, clientId2);

		assertAll("prometheus",
				() -> assertEquals(loginBefore + 3, loginAfter, "login success total"),
				() -> assertEquals(loginBefore1 + 2, loginAfter1, "login success #1"),
				() -> assertEquals(loginBefore2 + 1, loginAfter2, "login success #2"),
				() -> assertEquals(loginErrorBefore + 1, loginErrorAfter, "login failure total"),
				() -> assertEquals(loginErrorBefore1 + 0, loginErrorAfter1, "login failure #1"),
				() -> assertEquals(loginErrorBefore2 + 1, loginErrorAfter2, "login failure #2"));
	}
}
