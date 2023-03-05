package io.kokuwa.keycloak.metrics;

import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Factory for {@link MicrometerEventListener}, uses {@link MeterRegistry} from CDI.
 *
 * @author Stephan Schnabel
 */
public class MicrometerEventListenerFactory implements EventListenerProviderFactory {

	private static final Logger log = Logger.getLogger(MicrometerEventListener.class);
	private MeterRegistry registry;
	private boolean replace;

	@Override
	public String getId() {
		return "metrics-listener";
	}

	@Override
	public void init(Scope config) {
		replace = "true".equals(System.getenv("KC_METRICS_EVENT_REPLACE_IDS"));
		log.info(replace ? "Configured with model names." : "Configured with model ids.");
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		registry = CDI.current().select(MeterRegistry.class).get();
	}

	@Override
	public EventListenerProvider create(KeycloakSession session) {
		return new MicrometerEventListener(registry, session, replace);
	}

	@Override
	public void close() {}
}
