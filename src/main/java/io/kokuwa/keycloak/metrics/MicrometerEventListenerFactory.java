package io.kokuwa.keycloak.metrics;

import javax.enterprise.inject.spi.CDI;

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

	private MicrometerEventRecorder recorder;

	@Override
	public String getId() {
		return "metrics-listener";
	}

	@Override
	public void init(Scope config) {}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		recorder = new MicrometerEventRecorder(CDI.current().select(MeterRegistry.class).get());
	}

	@Override
	public EventListenerProvider create(KeycloakSession session) {
		return new MicrometerEventListener(recorder);
	}

	@Override
	public void close() {}
}
