package io.kokuwa.keycloak.metrics.stats;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;
import org.keycloak.timer.ScheduledTask;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tag;

/**
 * Keycloak metrics.
 *
 * @author Stephan Schnabel
 */
public class MetricsStatsTask implements Provider, ScheduledTask {

	private static final Logger log = Logger.getLogger(MetricsStatsTask.class);
	private static final Map<String, AtomicLong> values = new HashMap<>();
	private final Duration interval;
	private final Duration infoThreshold;
	private final Duration warnThreshold;

	MetricsStatsTask(Duration interval, Duration infoThreshold, Duration warnThreshold) {
		this.interval = interval;
		this.infoThreshold = infoThreshold;
		this.warnThreshold = warnThreshold;
	}

	@Override
	public void run(KeycloakSession session) {
		log.tracev("Triggered metrics stats task.");
		var start = Instant.now();

		try {
			scrape(session);
		} catch (Exception e) {
			if (e instanceof org.hibernate.exception.SQLGrammarException) {
				log.infov("Metrics status task skipped, database not ready");
			} else {
				log.errorv(e, "Failed to scrape stats.");
			}
			return;
		}

		var duration = Duration.between(start, Instant.now());
		if (duration.compareTo(interval) > 0) {
			log.errorv("Finished scrapping keycloak stats in {0}, consider to increase interval", duration);
		} else if (duration.compareTo(warnThreshold) > 0) {
			log.warnv("Finished scrapping keycloak stats in {0}, consider to increase interval", duration);
		} else if (duration.compareTo(infoThreshold) > 0) {
			log.infov("Finished scrapping keycloak stats in {0}", duration);
		} else {
			log.debugv("Finished scrapping keycloak stats in {0}", duration);
		}
	}

	@Override
	public void close() {}

	private void scrape(KeycloakSession session) {
		session.realms().getRealmsStream().forEach(realm -> {
			var tagRealm = Tag.of("realm", realm.getName());
			gauge("keycloak_users", Set.of(tagRealm), session.users().getUsersCount(realm));
			gauge("keycloak_clients", Set.of(tagRealm), session.clients().getClientsCount(realm));
			var sessions = session.sessions();
			var activeSessions = sessions.getActiveClientSessionStats(realm, false);
			realm.getClientsStream().forEach(client -> {
				var tags = Set.of(tagRealm, Tag.of("client", client.getClientId()));
				gauge("keycloak_offline_sessions", tags, sessions.getOfflineSessionsCount(realm, client));
				gauge("keycloak_active_user_sessions", tags, sessions.getActiveUserSessions(realm, client));
				gauge("keycloak_active_client_sessions", tags, activeSessions.getOrDefault(client.getId(), 0L));
			});
		});
	}

	private void gauge(String name, Set<Tag> tags, long value) {
		values.computeIfAbsent(name + tags, s -> Metrics.gauge(name, tags, new AtomicLong())).set(value);
	}
}
