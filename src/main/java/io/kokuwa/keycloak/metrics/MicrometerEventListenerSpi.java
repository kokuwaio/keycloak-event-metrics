package io.kokuwa.keycloak.metrics;

import org.keycloak.events.EventListenerSpi;
import org.keycloak.provider.Provider;

/**
 * Factory for {@link MicrometerEventListener}.
 *
 * @author Stephan Schnabel
 */
public class MicrometerEventListenerSpi extends EventListenerSpi {

	@Override
	public boolean isInternal() {
		return false;
	}

	@Override
	public String getName() {
		return "Micrometer Metrics Provider";
	}

	@Override
	public Class<? extends Provider> getProviderClass() {
		return MicrometerEventListener.class;
	}

	@Override
	public Class<MicrometerEventListenerFactory> getProviderFactoryClass() {
		return MicrometerEventListenerFactory.class;
	}
}
