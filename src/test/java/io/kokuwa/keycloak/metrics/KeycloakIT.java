package io.kokuwa.keycloak.metrics;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.ws.rs.NotAuthorizedException;

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

		var realmName1 = "loginAndAttempts_1";
		var clientId1 = realmName1 + "_client_1";
		var username1 = UUID.randomUUID().toString();
		var password1 = UUID.randomUUID().toString();
		keycloak.createRealm(realmName1);
		keycloak.createClient(realmName1, clientId1);
		keycloak.createUser(realmName1, username1, password1);

		var realmName2 = "loginAndAttempts_2";
		var clientId2 = realmName2 + "_client_2";
		var username2 = UUID.randomUUID().toString();
		var password2 = UUID.randomUUID().toString();
		keycloak.createRealm(realmName2);
		keycloak.createClient(realmName2, clientId2);
		keycloak.createUser(realmName2, username2, password2);

		var clientId3 = realmName2 + "_" + UUID.randomUUID();
		var clientId4 = realmName2 + "_" + UUID.randomUUID();

		prometheus.scrap();
		var loginBefore = prometheus.userEvent(EventType.LOGIN);
		var loginBefore1 = prometheus.userEvent(EventType.LOGIN, realmName1, clientId1);
		var loginBefore2 = prometheus.userEvent(EventType.LOGIN, realmName2, clientId2);
		var loginErrorBefore = prometheus.userEvent(EventType.LOGIN_ERROR);
		var loginErrorBefore1 = prometheus.userEvent(EventType.LOGIN_ERROR, realmName1, clientId1);
		var loginErrorBefore2 = prometheus.userEvent(EventType.LOGIN_ERROR, realmName2, clientId2);
		var loginErrorBeforeUNKNOWN = prometheus.userEvent(EventType.LOGIN_ERROR, realmName2, "UNKNOWN");

		assertDoesNotThrow(() -> keycloak.login(clientId1, realmName1, username1, password1));
		assertDoesNotThrow(() -> keycloak.login(clientId1, realmName1, username1, password1));
		assertDoesNotThrow(() -> keycloak.login(clientId2, realmName2, username2, password2));
		assertThrows(NotAuthorizedException.class, () -> keycloak.login(clientId3, realmName2, "nope", "nö"));
		assertThrows(NotAuthorizedException.class, () -> keycloak.login(clientId4, realmName2, "foo", "bar"));
		assertThrows(NotAuthorizedException.class, () -> keycloak.login(clientId2, realmName2, username2, "nope"));

		prometheus.scrap();
		var loginAfter = prometheus.userEvent(EventType.LOGIN);
		var loginAfter1 = prometheus.userEvent(EventType.LOGIN, realmName1, clientId1);
		var loginAfter2 = prometheus.userEvent(EventType.LOGIN, realmName2, clientId2);
		var loginErrorAfter = prometheus.userEvent(EventType.LOGIN_ERROR);
		var loginErrorAfter1 = prometheus.userEvent(EventType.LOGIN_ERROR, realmName1, clientId1);
		var loginErrorAfter2 = prometheus.userEvent(EventType.LOGIN_ERROR, realmName2, clientId2);
		var loginErrorAfter3 = prometheus.userEvent(EventType.LOGIN_ERROR, realmName2, clientId3);
		var loginErrorAfter4 = prometheus.userEvent(EventType.LOGIN_ERROR, realmName2, clientId4);
		var loginErrorAfterUNKNOWN = prometheus.userEvent(EventType.LOGIN_ERROR, realmName2, "UNKNOWN");

		assertAll("prometheus",
				() -> assertEquals(loginBefore + 3, loginAfter, "login success total"),
				() -> assertEquals(loginBefore1 + 2, loginAfter1, "login success #1"),
				() -> assertEquals(loginBefore2 + 1, loginAfter2, "login success #2"),
				() -> assertEquals(loginErrorBefore + 3, loginErrorAfter, "login failure total"),
				() -> assertEquals(loginErrorBefore1 + 0, loginErrorAfter1, "login failure #1"),
				() -> assertEquals(loginErrorBefore2 + 1, loginErrorAfter2, "login failure #2"),
				() -> assertEquals(0, loginErrorAfter3, "login failure #3"),
				() -> assertEquals(0, loginErrorAfter4, "login failure #4"),
				() -> assertEquals(loginErrorBeforeUNKNOWN + 2, loginErrorAfterUNKNOWN, "login failure UNKNOWN"));
	}

	@DisplayName("user count")
	@Test
	void userCount(KeycloakClient keycloak, Prometheus prometheus) {

		var realmName1 = "userCount_1";
		var realmName2 = "userCount_2";
		var username = UUID.randomUUID().toString();

		keycloak.createRealm(realmName1);
		keycloak.createRealm(realmName2);

		await(() -> prometheus.userCount(realmName1) == 0, prometheus, "realm 1 not found");
		await(() -> prometheus.userCount(realmName2) == 0, prometheus, "realm 2 not found");

		keycloak.createUser(realmName1, username, UUID.randomUUID().toString());
		keycloak.createUser(realmName1, UUID.randomUUID().toString(), UUID.randomUUID().toString());
		keycloak.createUser(realmName1, UUID.randomUUID().toString(), UUID.randomUUID().toString());
		keycloak.createUser(realmName2, UUID.randomUUID().toString(), UUID.randomUUID().toString());

		await(() -> prometheus.userCount(realmName1) == 3, prometheus, "realm 1 shoud have 3 users");
		await(() -> prometheus.userCount(realmName2) == 1, prometheus, "realm 2 shoud have 1 users");

		keycloak.deleteUser(realmName1, username);

		await(() -> prometheus.userCount(realmName1) == 2, prometheus, "realm 1 shoud have 2 users after deletion");
		await(() -> prometheus.userCount(realmName2) == 1, prometheus, "realm 2 shoud have 1 users");
	}

	void await(Supplier<Boolean> check, Prometheus prometheus, String message) {
		var end = Instant.now().plusSeconds(10);
		while (Instant.now().isBefore(end) && !check.get()) {
			assertDoesNotThrow(() -> Thread.sleep(1000));
			prometheus.scrap();
		}
		assertTrue(check.get(), message);
	}
}
