package io.kokuwa.keycloak.metrics;

import java.util.Optional;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

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

	MicrometerEventListener(MeterRegistry registry, KeycloakSession session, boolean replaceId) {
		this.registry = registry;
		this.session = session;
		this.replace = replaceId;
	}

	@Override
	public void onEvent(Event event) {
		registry.counter("keycloak_event_user",
				"realm", toBlank(replace ? getRealmName(event.getRealmId()) : event.getRealmId()),
				"type", toBlank(event.getType()),
				"client", toBlank(event.getClientId()),
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
		return Optional.ofNullable(session.getContext()).map(KeycloakContext::getRealm)
				.filter(realm -> id == null || id.equals(realm.getId()))
				.or(() -> {
					log.tracev("Context realm was empty with id {0}", id);
					return Optional.ofNullable(id).map(session.realms()::getRealm);
				})
				.map(RealmModel::getName)
				.orElseGet(() -> {
					log.warnv("Failed to find realm with id {0}", id);
					return id;
				});
	}

	private String toBlank(Object value) {
		return value == null ? "" : value.toString();
	}
}
