package io.kokuwa.keycloak.metrics.event;

import javax.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Factory for {@link MetricsEventListener}, uses {@link MeterRegistry} from CDI.
 *
 * @author Stephan Schnabel
 */
public class MetricsEventListenerFactory implements EventListenerProviderFactory {

	private static final Logger log = Logger.getLogger(MetricsEventListenerFactory.class);
	private MeterRegistry registry;
	private boolean replaceIds;

	@Override
	public String getId() {
		return "metrics-listener";
	}

	@Override
	public void init(Scope config) {
		replaceIds = "true".equals(System.getenv().getOrDefault("KC_METRICS_EVENT_REPLACE_IDS", "true"));
		log.info(replaceIds ? "Configured with model names." : "Configured with model ids.");
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		registry = CDI.current().select(MeterRegistry.class).get();
	}

	@Override
	public EventListenerProvider create(KeycloakSession session) {
		return new MetricsEventListener(registry, replaceIds, session);
	}

	@Override
	public void close() {}
}
