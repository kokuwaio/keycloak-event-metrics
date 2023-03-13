package io.kokuwa.keycloak.metrics;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakSession;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Listener for {@link Event} and {@link AdminEvent}.
 *
 * @author Stephan Schnabel
 */
public class MicrometerEventListener implements EventListenerProvider, AutoCloseable {

	private static final Logger log = Logger.getLogger(MicrometerEventListener.class);
	private final MeterRegistry registry;
	private final KeycloakSession session;
	private final boolean replace;

	public MicrometerEventListener(MeterRegistry registry, KeycloakSession session, boolean replaceId) {
		this.registry = registry;
		this.session = session;
		this.replace = replaceId;
	}

	@Override
	public void onEvent(Event event) {
		registry.counter("keycloak_event_user",
				"realm", toBlank(replace ? getRealmName(event.getRealmId()) : event.getRealmId()),
				"type", toBlank(event.getType()),
				"client", toBlank(replace ? getClientId(event.getClientId()) : event.getClientId()),
				"error", toBlank(event.getError()))
				.increment();
	}

	@Override
	public void onEvent(AdminEvent event, boolean includeRepresentation) {
		registry.counter("keycloak_event_admin",
				"realm", toBlank(replace ? getRealmName(event.getRealmId()) : event.getRealmId()),
				"resource", toBlank(event.getResourceType()),
				"operation", toBlank(event.getOperationType()),
				"error", toBlank(event.getError()))
				.increment();
	}

	@Override
	public void close() {}

	private String getRealmName(String id) {
		var model = session.getContext().getRealm();
		if (id == null || id.equals(model.getId())) {
			return model.getName();
		}
		log.warnv("Failed to resolve realmName for id {0}", id);
		return id;
	}

	private String getClientId(String id) {
		var model = session.getContext().getClient();
		if (id == null || id.equals(model.getId())) {
			return model.getClientId();
		}
		log.warnv("Failed to resolve clientId for id {0}", id);
		return id;
	}

	private String toBlank(Object value) {
		return value == null ? "" : value.toString();
	}
}
