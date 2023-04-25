package io.kokuwa.keycloak.metrics.stats;

import java.time.Duration;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.TimerProvider;

import io.micrometer.core.instrument.Metrics;

/**
 * Implementation of {@link MetricsStatsFactory}.
 *
 * @author Stephan Schnabel
 */
public class MetricsStatsFactoryImpl implements MetricsStatsFactory {

	private static final Logger log = Logger.getLogger(MetricsStatsFactory.class);

	@Override
	public String getId() {
		return "default";
	}

	@Override
	public void init(Scope config) {}

	@Override
	public void postInit(KeycloakSessionFactory factory) {

		if (!"true".equals(System.getenv().get("KC_METRICS_STATS_ENABLED"))) {
			log.infov("Keycloak stats not enabled.");
			return;
		}

		var intervalDuration = Optional
				.ofNullable(System.getenv("KC_METRICS_STATS_INTERVAL"))
				.map(Duration::parse)
				.orElse(Duration.ofSeconds(60));
		var infoThreshold = Optional
				.ofNullable(System.getenv("KC_METRICS_STATS_INFO_THRESHOLD"))
				.map(Duration::parse)
				.orElse(Duration.ofMillis(Double.valueOf(intervalDuration.toMillis() * 0.5).longValue()));
		var warnThreshold = Optional
				.ofNullable(System.getenv("KC_METRICS_STATS_WARN_THRESHOLD"))
				.map(Duration::parse)
				.orElse(Duration.ofMillis(Double.valueOf(intervalDuration.toMillis() * 0.75).longValue()));
		log.infov("Keycloak stats enabled with interval of {0} and info/warn after {1}/{2}.",
				intervalDuration, infoThreshold, warnThreshold);

		var interval = intervalDuration.toMillis();
		var task = new MetricsStatsTask(Metrics.globalRegistry, intervalDuration, infoThreshold, warnThreshold);
		KeycloakModelUtils.runJobInTransaction(factory, session -> session
				.getProvider(TimerProvider.class)
				.schedule(() -> KeycloakModelUtils.runJobInTransaction(factory, task), interval, "metrics"));
	}

	@Override
	public MetricsStatsTask create(KeycloakSession session) {
		return null;
	}

	@Override
	public void close() {}
}
