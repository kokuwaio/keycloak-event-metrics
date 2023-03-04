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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
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

	@InjectMocks
	MicrometerEventListener listener;
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

	@DisplayName("onEvent(Event)")
	@Nested
	class onEvent {

		@DisplayName("without error")
		@Test
		void withoutError() {

			var realmId = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var type = EventType.LOGIN;

			listener.onEvent(toEvent(realmId, clientId, type, null));
			assertEvent(realmId, clientId, type.toString(), "");
		}

		@DisplayName("with error")
		@Test
		void withError() {

			var realmId = UUID.randomUUID().toString();
			var clientId = UUID.randomUUID().toString();
			var type = EventType.LOGIN_ERROR;
			var error = UUID.randomUUID().toString();

			listener.onEvent(toEvent(realmId, clientId, type, error));
			assertEvent(realmId, clientId, type.toString(), error);
		}

		@DisplayName("all fields empty")
		@Test
		void fieldsEmpty() {
			listener.onEvent(toEvent(null, null, null, null));
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

		@DisplayName("without error")
		@Test
		void withoutError() {

			var realmId = UUID.randomUUID().toString();
			var resource = ResourceType.USER;
			var operation = OperationType.CREATE;

			listener.onEvent(toAdminEvent(realmId, resource, operation, null), false);
			assertAdminEvent(realmId, resource.toString(), operation.toString(), "");
		}

		@DisplayName("with error")
		@Test
		void withError() {

			var realmId = UUID.randomUUID().toString();
			var resource = ResourceType.USER;
			var operation = OperationType.CREATE;
			var error = UUID.randomUUID().toString();

			listener.onEvent(toAdminEvent(realmId, resource, operation, error), false);
			assertAdminEvent(realmId, resource.toString(), operation.toString(), error);
		}

		@DisplayName("all fields empty")
		@Test
		void fieldsEmpty() {
			listener.onEvent(toAdminEvent(null, null, null, null), false);
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

	private void assertCounter(String metric, Map<String, String> tags) {
		verify(registry).counter(anyString(), any(String[].class));
		verify(counter).increment();
		assertEquals(metric, metricCaptor.getValue(), "metric");
		assertEquals(tags, IntStream
				.range(0, tagsCaptor.getValue().length / 2).mapToObj(i -> i * 2)
				.collect(Collectors.toMap(i -> tagsCaptor.getValue()[i], i -> tagsCaptor.getValue()[i + 1])), "tags");
	}
}
