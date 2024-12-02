package io.kokuwa.keycloak.metrics.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.mockito.Mock;

import io.kokuwa.keycloak.metrics.junit.AbstractMockitoTest;
import io.micrometer.core.instrument.Metrics;

/**
 * Test for {@link MetricsEventListener} with Mockito.
 *
 * @author Stephan Schnabel
 */
@DisplayName("events: listener")
public class MetricsEventListenerTest extends AbstractMockitoTest {

	@Mock
	KeycloakSession session;
	@Mock
	RealmModel realmModel;
	@Mock
	RealmProvider realmProvider;
	@Mock
	ClientModel clientModel;
	@Mock
	KeycloakContext context;

	@DisplayName("onEvent(true)")
	@Nested
	class onEvent {

		@DisplayName("replace(true) - without error")
		@Test
		void replaceWithoutError() {

			var realmId = UUID.randomUUID().toString();
			var realmName = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var type = EventType.LOGIN;

			when(session.getContext()).thenReturn(context);
			when(context.getRealm()).thenReturn(realmModel);
			when(context.getClient()).thenReturn(clientModel);
			when(realmModel.getId()).thenReturn(realmId);
			when(realmModel.getName()).thenReturn(realmName);
			when(clientModel.getClientId()).thenReturn(clientId);

			listener(true).onEvent(toEvent(realmId, clientId, type, null));
			assertEvent(realmName, clientId, type.toString(), "");
		}

		@DisplayName("replace(true) - with error")
		@Test
		void replaceWithError() {

			var realmId = UUID.randomUUID().toString();
			var realmName = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var type = EventType.LOGIN_ERROR;
			var error = UUID.randomUUID().toString();

			when(session.getContext()).thenReturn(context);
			when(context.getRealm()).thenReturn(realmModel);
			when(context.getClient()).thenReturn(clientModel);
			when(realmModel.getId()).thenReturn(realmId);
			when(realmModel.getName()).thenReturn(realmName);
			when(clientModel.getClientId()).thenReturn(clientId);

			listener(true).onEvent(toEvent(realmId, clientId, type, error));
			assertEvent(realmName, clientId, type.toString(), error);
		}

		@DisplayName("replace(true) - all fields empty")
		@Test
		void replaceFieldsEmpty() {

			var realmName = UUID.randomUUID().toString();

			when(session.getContext()).thenReturn(context);
			when(context.getRealm()).thenReturn(realmModel);
			when(realmModel.getName()).thenReturn(realmName);

			listener(true).onEvent(toEvent(null, null, null, null));
			assertEvent(realmName, "UNKNOWN", "", "");
		}

		@DisplayName("replace(true) - context is null")
		@Test
		void replaceFieldsContextNull() {

			var realmId = UUID.randomUUID().toString();
			var realmName = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var type = EventType.LOGIN_ERROR;

			when(session.realms()).thenReturn(realmProvider);
			when(realmProvider.getRealm(realmId)).thenReturn(realmModel);
			when(realmModel.getName()).thenReturn(realmName);

			listener(true).onEvent(toEvent(realmId, clientId, type, null));
			assertEvent(realmName, "UNKNOWN", type.toString(), "");
		}

		@DisplayName("replace(true) - context is empty")
		@Test
		void replaceFieldsContextEmpty() {

			var realmId = UUID.randomUUID().toString();
			var realmName = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var type = EventType.LOGIN_ERROR;

			when(session.getContext()).thenReturn(context);
			when(session.realms()).thenReturn(realmProvider);
			when(realmProvider.getRealm(realmId)).thenReturn(realmModel);
			when(realmModel.getName()).thenReturn(realmName);

			listener(true).onEvent(toEvent(realmId, clientId, type, null));
			assertEvent(realmName, "UNKNOWN", type.toString(), "");
		}

		@DisplayName("replace(true) - realmId is unknown")
		@Test
		void replaceFieldsRealmIdUnknown() {

			var realmId = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var type = EventType.LOGIN_ERROR;

			when(session.getContext()).thenReturn(context);
			when(session.realms()).thenReturn(realmProvider);
			when(context.getRealm()).thenReturn(realmModel);
			when(context.getClient()).thenReturn(clientModel);
			when(realmModel.getId()).thenReturn(UUID.randomUUID().toString());
			when(clientModel.getClientId()).thenReturn(clientId);

			listener(true).onEvent(toEvent(realmId, clientId, type, null));
			assertEvent(realmId, clientId, type.toString(), "");
		}

		@DisplayName("replace(false) - without error")
		@Test
		void notReplaceWithoutError() {

			var realmId = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var type = EventType.LOGIN;

			listener(false).onEvent(toEvent(realmId, clientId, type, null));
			assertEvent(realmId, "UNKNOWN", type.toString(), "");
		}

		@DisplayName("replace(false) - with error")
		@Test
		void notReplaceWithError() {

			var realmId = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var type = EventType.LOGIN_ERROR;
			var error = UUID.randomUUID().toString();

			listener(false).onEvent(toEvent(realmId, clientId, type, error));
			assertEvent(realmId, "UNKNOWN", type.toString(), error);
		}

		@DisplayName("replace(false) - all fields empty")
		@Test
		void notReplaceFieldsEmpty() {
			listener(false).onEvent(toEvent(null, null, null, null));
			assertEvent("", "UNKNOWN", "", "");
		}

		private Event toEvent(String realmId, String clientId, EventType type, String error) {
			var event = new Event();
			event.setRealmId(realmId);
			event.setClientId(clientId);
			event.setType(type);
			event.setError(error);
			return event;
		}

		private void assertEvent(String realm, String client, String type, String error) {
			assertCounter("keycloak_event_user",
					"realm", realm,
					"client", client,
					"type", type,
					"error", error);
		}
	}

	@DisplayName("onEvent(AdminEvent,boolean)")
	@Nested
	class onAdminEvent {

		@DisplayName("replace(true) - without error")
		@Test
		void replaceWithoutError() {

			var realmId = UUID.randomUUID().toString();
			var realmName = UUID.randomUUID().toString();
			var resource = ResourceType.USER;
			var operation = OperationType.CREATE;

			when(session.getContext()).thenReturn(context);
			when(context.getRealm()).thenReturn(realmModel);
			when(realmModel.getId()).thenReturn(realmId);
			when(realmModel.getName()).thenReturn(realmName);

			listener(true).onEvent(toAdminEvent(realmId, resource, operation, null), false);
			assertAdminEvent(realmName, resource.toString(), operation.toString(), "");
		}

		@DisplayName("replace(true) - with error")
		@Test
		void replaceWithError() {

			var realmId = UUID.randomUUID().toString();
			var realmName = UUID.randomUUID().toString();
			var resource = ResourceType.USER;
			var operation = OperationType.CREATE;
			var error = UUID.randomUUID().toString();

			when(session.getContext()).thenReturn(context);
			when(context.getRealm()).thenReturn(realmModel);
			when(realmModel.getId()).thenReturn(realmId);
			when(realmModel.getName()).thenReturn(realmName);

			listener(true).onEvent(toAdminEvent(realmId, resource, operation, error), false);
			assertAdminEvent(realmName, resource.toString(), operation.toString(), error);
		}

		@DisplayName("replace(true) - all fields empty")
		@Test
		void replaceFieldsEmpty() {

			var realmName = UUID.randomUUID().toString();

			when(session.getContext()).thenReturn(context);
			when(context.getRealm()).thenReturn(realmModel);
			when(realmModel.getName()).thenReturn(realmName);

			listener(true).onEvent(toAdminEvent(null, null, null, null), false);
			assertAdminEvent(realmName, "", "", "");
		}

		@DisplayName("replace(true) - context is null")
		@Test
		void replaceFieldsContextNull() {

			var realmId = UUID.randomUUID().toString();
			var realmName = UUID.randomUUID().toString();
			var resource = ResourceType.USER;
			var operation = OperationType.CREATE;

			when(session.realms()).thenReturn(realmProvider);
			when(realmProvider.getRealm(realmId)).thenReturn(realmModel);
			when(realmModel.getName()).thenReturn(realmName);

			listener(true).onEvent(toAdminEvent(realmId, resource, operation, null), false);
			assertAdminEvent(realmName, resource.toString(), operation.toString(), "");
		}

		@DisplayName("replace(true) - context is empty")
		@Test
		void replaceFieldsContextEmpty() {

			var realmId = UUID.randomUUID().toString();
			var realmName = UUID.randomUUID().toString();
			var resource = ResourceType.USER;
			var operation = OperationType.CREATE;

			when(session.getContext()).thenReturn(context);
			when(session.realms()).thenReturn(realmProvider);
			when(realmProvider.getRealm(realmId)).thenReturn(realmModel);
			when(realmModel.getName()).thenReturn(realmName);

			listener(true).onEvent(toAdminEvent(realmId, resource, operation, null), false);
			assertAdminEvent(realmName, resource.toString(), operation.toString(), "");
		}

		@DisplayName("replace(true) - realmId is unknown")
		@Test
		void replaceFieldsRealmIdUnknown() {

			var realmId = UUID.randomUUID().toString();
			var resource = ResourceType.USER;
			var operation = OperationType.CREATE;

			when(session.getContext()).thenReturn(context);
			when(session.realms()).thenReturn(realmProvider);
			when(context.getRealm()).thenReturn(realmModel);
			when(realmModel.getId()).thenReturn(UUID.randomUUID().toString());

			listener(true).onEvent(toAdminEvent(realmId, resource, operation, null), false);
			assertAdminEvent(realmId, resource.toString(), operation.toString(), "");
		}

		@DisplayName("replace(false) - without error")
		@Test
		void noReplaceWithoutError() {

			var realmId = UUID.randomUUID().toString();
			var resource = ResourceType.USER;
			var operation = OperationType.CREATE;

			listener(false).onEvent(toAdminEvent(realmId, resource, operation, null), false);
			assertAdminEvent(realmId, resource.toString(), operation.toString(), "");
		}

		@DisplayName("replace(false) - with error")
		@Test
		void noReplaceWithError() {

			var realmId = UUID.randomUUID().toString();
			var resource = ResourceType.USER;
			var operation = OperationType.CREATE;
			var error = UUID.randomUUID().toString();

			listener(false).onEvent(toAdminEvent(realmId, resource, operation, error), false);
			assertAdminEvent(realmId, resource.toString(), operation.toString(), error);
		}

		@DisplayName("replace(false) - all fields empty")
		@Test
		void noReplaceFieldsEmpty() {
			listener(false).onEvent(toAdminEvent(null, null, null, null), false);
			assertAdminEvent("", "", "", "");
		}

		private AdminEvent toAdminEvent(String realmId, ResourceType resource, OperationType operation, String error) {
			var event = new AdminEvent();
			event.setRealmId(realmId);
			event.setResourceType(resource);
			event.setOperationType(operation);
			event.setError(error);
			return event;
		}

		private void assertAdminEvent(String realm, String resource, String operation, String error) {
			assertCounter("keycloak_event_admin",
					"realm", realm,
					"resource", resource,
					"operation", operation,
					"error", error);
		}
	}

	private MetricsEventListener listener(boolean replace) {
		return new MetricsEventListener(replace, session);
	}

	private static void assertCounter(String metric, String... tags) {
		var counter = Metrics.globalRegistry.counter(metric, tags);
		assertEquals(1D, counter.count(), "micrometer.counter.count");
		assertEquals(0, Metrics.globalRegistry
				.getMeters().stream()
				.filter(meter -> meter != counter)
				.count(),
				"other meter found");
	}
}
