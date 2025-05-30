package io.kokuwa.keycloak.metrics.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedHashMap;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.token.TokenService;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Client for keycloak.
 *
 * @author Stephan Schnabel
 */
public class KeycloakClient {

	private final Keycloak keycloak;
	private final TokenService tokenService;

	private final ObjectMapper mapper = new ObjectMapper();
	private final HttpClient client = HttpClient.newHttpClient();
	private final String url;
	private final String adminToken;

	KeycloakClient(String url, Keycloak keycloak, TokenService tokenService) {
		this.keycloak = keycloak;
		this.tokenService = tokenService;
		this.url = url;
		this.adminToken = login("admin-cli", "master", "admin", "password").getToken();
	}

	public void createRealm(String realmName) {
		var realm = new RealmRepresentation();
		realm.setId(UUID.randomUUID().toString());
		realm.setEnabled(true);
		realm.setRealm(realmName);
		realm.setEventsListeners(List.of("metrics-listener"));
		keycloak.realms().create(realm);
	}

	public void createClient(String realmName, String clientId) {
		var client = new ClientRepresentation();
		client.setId(UUID.randomUUID().toString());
		client.setClientId(clientId);
		client.setPublicClient(true);
		client.setDirectAccessGrantsEnabled(true);
		var response = keycloak.realms().realm(realmName).clients().create(client);
		assertEquals(201, response.getStatus());
	}

	public void createUser(String realmName, String username, String password) {
		try {
			var response = client.send(HttpRequest.newBuilder()
					.uri(URI.create(url + "/admin/realms/" + realmName + "/users"))
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
					.POST(BodyPublishers.ofString(mapper.writeValueAsString(Map.of(
							"enabled", true,
							"emailVerified", true,
							"email", username + "@example.org",
							"username", username,
							"firstName", username,
							"lastName", username,
							"credentials", List.of(Map.of(
									"type", CredentialRepresentation.PASSWORD,
									"value", password,
									"temporary", false))))))
					.build(), BodyHandlers.ofString());
			assertEquals(201, response.statusCode(), "Body: " + response.body());
		} catch (IOException | InterruptedException e) {
			fail("Failed to create user", e);
		}
	}

	public void deleteUser(String realmName, String username) {
		keycloak.realms().realm(realmName).users()
				.searchByUsername(username, true).stream()
				.map(UserRepresentation::getId)
				.forEach(keycloak.realms().realm(realmName).users()::delete);
	}

	public AccessTokenResponse login(String clientId, String realmName, String username, String password) {
		return tokenService.grantToken(realmName, new MultivaluedHashMap<>(Map.of(
				OAuth2Constants.CLIENT_ID, clientId,
				OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD,
				OAuth2Constants.USERNAME, username,
				OAuth2Constants.PASSWORD, password)));
	}
}
