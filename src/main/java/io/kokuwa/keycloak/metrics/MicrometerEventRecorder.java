package io.kokuwa.keycloak.metrics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer based recorder for events.
 *
 * @author Stephan Schnabel
 */
public class MicrometerEventRecorder {

	private final Map<String, Counter> counters = new HashMap<>();
	private final MeterRegistry registry;

	MicrometerEventRecorder(MeterRegistry registry) {
		this.registry = registry;
	}

	void adminEvent(AdminEvent event) {
		counter("keycloak_event_admin",
				"realm", toBlankIfNull(event.getRealmId()),
				"resource", toBlankIfNull(event.getResourceType()),
				"operation", toBlankIfNull(event.getOperationType()),
				"error", toBlankIfNull(event.getError()));
	}

	void userEvent(Event event) {
		counter("keycloak_event_user",
				"realm", toBlankIfNull(event.getRealmId()),
				"type", toBlankIfNull(event.getType()),
				"client", toBlankIfNull(event.getClientId()),
				"error", toBlankIfNull(event.getError()));
	}

	private void counter(String counter, String... tags) {
		counters.computeIfAbsent(counter + Arrays.toString(tags), string -> registry.counter(counter, tags))
				.increment();
	}

	private String toBlankIfNull(Object value) {
		return value == null ? "" : value.toString();
	}
}
