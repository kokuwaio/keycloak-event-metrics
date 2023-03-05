package io.kokuwa.keycloak.metrics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Test for {@link MicrometerEventListener} with Mockito.
 *
 * @author Stephan Schnabel
 */
@ExtendWith(MockitoExtension.class)
public class MicrometerEventListenerTest {

	@Mock
	KeycloakSession session;
	@Mock
	RealmProvider realmProvider;
	@Mock
	RealmModel realmModel;
	@Mock
	ClientModel clientModel;
	@Mock
	MeterRegistry registry;
	@Mock
	Counter counter;
	@Captor
	ArgumentCaptor<String> metricCaptor;
	@Captor
	ArgumentCaptor<String[]> tagsCaptor;

	@BeforeEach
	void setup() {
		when(registry.counter(metricCaptor.capture(), tagsCaptor.capture())).thenReturn(counter);
	}

	@DisplayName("onEvent(true)")
	@Nested
	class onEvent {

		@DisplayName("replace(true) - without error")
		@Test
		void replaceWithoutError() {

			var realmId = UUID.randomUUID().toString();
			var realmName = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var clientName = UUID.randomUUID().toString();
			var type = EventType.LOGIN;

			when(session.realms()).thenReturn(realmProvider);
			when(realmProvider.getRealm(realmId)).thenReturn(realmModel);
			when(realmModel.getName()).thenReturn(realmName);
			when(realmModel.getClientById(clientId)).thenReturn(clientModel);
			when(clientModel.getClientId()).thenReturn(clientName);

			listener(true).onEvent(toEvent(realmId, clientId, type, null));
			assertEvent(realmName, clientName, type.toString(), "");
		}

		@DisplayName("replace(true) - with error")
		@Test
		void replaceWithError() {

			var realmId = UUID.randomUUID().toString();
			var realmName = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var clientName = UUID.randomUUID().toString();
			var type = EventType.LOGIN_ERROR;
			var error = UUID.randomUUID().toString();

			when(session.realms()).thenReturn(realmProvider);
			when(realmProvider.getRealm(realmId)).thenReturn(realmModel);
			when(realmModel.getName()).thenReturn(realmName);
			when(realmModel.getClientById(clientId)).thenReturn(clientModel);
			when(clientModel.getClientId()).thenReturn(clientName);

			listener(true).onEvent(toEvent(realmId, clientId, type, error));
			assertEvent(realmName, clientName, type.toString(), error);
		}

		@DisplayName("replace(true) - all fields empty")
		@Test
		void replaceFieldsEmpty() {

			when(session.realms()).thenReturn(realmProvider);
			when(realmProvider.getRealm(any())).thenReturn(null);

			listener(true).onEvent(toEvent(null, null, null, null));
			assertEvent("", "", "", "");
		}

		@DisplayName("replace(false) - without error")
		@Test
		void notReplaceWithoutError() {

			var realmId = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var type = EventType.LOGIN;

			listener(false).onEvent(toEvent(realmId, clientId, type, null));
			assertEvent(realmId, clientId, type.toString(), "");
		}

		@DisplayName("replace(false) - with error")
		@Test
		void notReplaceWithError() {

			var realmId = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var type = EventType.LOGIN_ERROR;
			var error = UUID.randomUUID().toString();

			listener(false).onEvent(toEvent(realmId, clientId, type, error));
			assertEvent(realmId, clientId, type.toString(), error);
		}

		@DisplayName("replace(false) - all fields empty")
		@Test
		void notReplaceFieldsEmpty() {
			listener(false).onEvent(toEvent(null, null, null, null));
			assertEvent("", "", "", "");
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
			assertCounter("keycloak_event_user", Map.of(
					"realm", realm,
					"client", client,
					"type", type,
					"error", error));
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

			when(session.realms()).thenReturn(realmProvider);
			when(realmProvider.getRealm(realmId)).thenReturn(realmModel);
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

			when(session.realms()).thenReturn(realmProvider);
			when(realmProvider.getRealm(realmId)).thenReturn(realmModel);
			when(realmModel.getName()).thenReturn(realmName);

			listener(true).onEvent(toAdminEvent(realmId, resource, operation, error), false);
			assertAdminEvent(realmName, resource.toString(), operation.toString(), error);
		}

		@DisplayName("replace(true) - all fields empty")
		@Test
		void replaceFieldsEmpty() {

			when(session.realms()).thenReturn(realmProvider);
			when(realmProvider.getRealm(any())).thenReturn(null);

			listener(true).onEvent(toAdminEvent(null, null, null, null), false);
			assertAdminEvent("", "", "", "");
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
			assertCounter("keycloak_event_admin", Map.of(
					"realm", realm,
					"resource", resource,
					"operation", operation,
					"error", error));
		}
	}

	private MicrometerEventListener listener(boolean replace) {
		return new MicrometerEventListener(registry, session, replace);
	}

	private void assertCounter(String metric, Map<String, String> tags) {
		verify(registry).counter(anyString(), any(String[].class));
		verify(counter).increment();
		assertEquals(metric, metricCaptor.getValue(), "metric");
		assertEquals(tags, IntStream
				.range(0, tagsCaptor.getValue().length / 2).mapToObj(i -> i * 2)
				.collect(Collectors.toMap(i -> tagsCaptor.getValue()[i], i -> tagsCaptor.getValue()[i + 1])), "tags");
	}
}
