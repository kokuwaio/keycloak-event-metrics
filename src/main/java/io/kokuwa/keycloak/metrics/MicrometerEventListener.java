package io.kokuwa.keycloak.metrics;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Listener for {@link Event} and {@link AdminEvent}.
 *
 * @author Stephan Schnabel
 */
public class MicrometerEventListener implements EventListenerProvider, AutoCloseable {

	private final MeterRegistry registry;

	public MicrometerEventListener(MeterRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void onEvent(Event event) {
		registry.counter("keycloak_event_user",
				"realm", toBlank(event.getRealmId()),
				"type", toBlank(event.getType()),
				"client", toBlank(event.getClientId()),
				"error", toBlank(event.getError()))
				.increment();
	}

	@Override
	public void onEvent(AdminEvent event, boolean includeRepresentation) {
		registry.counter("keycloak_event_admin",
				"realm", toBlank(event.getRealmId()),
				"resource", toBlank(event.getResourceType()),
				"operation", toBlank(event.getOperationType()),
				"error", toBlank(event.getError()))
				.increment();
	}

	@Override
	public void close() {}

	private String toBlank(Object value) {
		return value == null ? "" : value.toString();
	}
}
