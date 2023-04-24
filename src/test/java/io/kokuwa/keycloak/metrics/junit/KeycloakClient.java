package io.kokuwa.keycloak.metrics.junit;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.MultivaluedHashMap;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.token.TokenService;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

/**
 * Client for keycloak.
 *
 * @author Stephan Schnabel
 */
public class KeycloakClient {

	private final Keycloak keycloak;
	private final TokenService token;

	KeycloakClient(Keycloak keycloak, TokenService token) {
		this.keycloak = keycloak;
		this.token = token;
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
		keycloak.realms().realm(realmName).clients().create(client);
	}

	public void createUser(String realmName, String username, String password) {
		var credential = new CredentialRepresentation();
		credential.setType(CredentialRepresentation.PASSWORD);
		credential.setValue(password);
		credential.setTemporary(false);
		var user = new UserRepresentation();
		user.setEnabled(true);
		user.setEmail(username + "@example.org");
		user.setEmailVerified(true);
		user.setUsername(username);
		user.setCredentials(List.of(credential));
		keycloak.realms().realm(realmName).users().create(user);
	}

	public boolean login(String clientId, String realmName, String username, String password) {
		try {
			token.grantToken(realmName, new MultivaluedHashMap<>(Map.of(
					OAuth2Constants.CLIENT_ID, clientId,
					OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD,
					OAuth2Constants.USERNAME, username,
					OAuth2Constants.PASSWORD, password)));
			return true;
		} catch (NotAuthorizedException e) {
			return false;
		}
	}
}
