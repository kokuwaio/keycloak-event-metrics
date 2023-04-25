package io.kokuwa.keycloak.metrics.stats;

import org.keycloak.provider.ProviderFactory;

/**
 * Factory for Keycloak metrics.
 *
 * @author Stephan Schnabel
 */
public interface MetricsStatsFactory extends ProviderFactory<MetricsStatsTask> {}
