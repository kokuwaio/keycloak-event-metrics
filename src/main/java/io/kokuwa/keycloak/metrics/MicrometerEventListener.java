package io.kokuwa.keycloak.metrics;

import java.util.Optional;

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
				"client", toBlank(replace ? getClientId(event.getRealmId(), event.getClientId()) : event.getClientId()),
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

	private String toBlank(Object value) {
		return value == null ? "" : value.toString();
	}

	private String getRealmName(String realmId) {
		var model = session.realms().getRealm(realmId);
		if (model == null) {
			log.warnv("Failed to resolve realm with id", realmId);
			return realmId;
		}
		return model.getName();
	}

	private String getClientId(String realmId, String clientId) {
		var model = Optional.ofNullable(session.realms().getRealm(realmId))
				.map(realm -> realm.getClientById(clientId))
				.orElse(null);
		if (model == null) {
			log.warnv("Failed to resolve client with id {} in realm {}", clientId, realmId);
			return clientId;
		}
		return model.getClientId();
	}
}
