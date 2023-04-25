package io.kokuwa.keycloak.metrics.stats;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/**
 * SPI for Keycloak metrics.
 *
 * @author Stephan Schnabel
 */
public class MetricsStatsSpi implements Spi {

	@Override
	public boolean isInternal() {
		return false;
	}

	@Override
	public String getName() {
		return "metrics";
	}

	@Override
	public Class<? extends Provider> getProviderClass() {
		return MetricsStatsTask.class;
	}

	@Override
	public Class<? extends ProviderFactory<? extends Provider>> getProviderFactoryClass() {
		// this must be an interface, otherwise spi will be silenty ignored
		return MetricsStatsFactory.class;
	}
}
