package io.kokuwa.keycloak.metrics.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Stream;

import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionProvider;
import org.mockito.Mock;

import io.kokuwa.keycloak.metrics.junit.AbstractMockitoTest;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;

/**
 * Test for {@link MetricsStatsTask} with Mockito.
 *
 * @author Stephan Schnabel
 */
@DisplayName("metrics: task")
public class MetricsStatsTaskTest extends AbstractMockitoTest {

	@Mock
	KeycloakSession session;
	@Mock
	RealmProvider realmProvider;
	@Mock
	UserProvider userProvider;
	@Mock
	UserSessionProvider sessionProvider;
	@Mock
	ClientProvider clientProvider;

	@BeforeEach
	void setup() {
		when(session.realms()).thenReturn(realmProvider);
	}

	@DisplayName("catch - nullpointer")
	@Test
	void catchNPE() {
		when(session.realms()).thenThrow(NullPointerException.class);
		task().run(session);
		assertLog(Level.SEVERE, "Failed to scrape stats.");
	}

	@DisplayName("catch - database")
	@Test
	void catchDatabase() {
		when(session.realms()).thenThrow(SQLGrammarException.class);
		task().run(session);
		assertLog(Level.INFO, "Metrics status task skipped, database not ready.");
	}

	@DisplayName("log - debug")
	@Test
	void logDebug() {
		when(realmProvider.getRealmsStream()).thenReturn(Stream.of());
		task(Duration.ofMillis(300), Duration.ofMillis(100), Duration.ofMillis(200)).run(session);
		assertLog(Level.FINE, "Finished scrapping keycloak stats in {0}.");
	}

	@DisplayName("log - info")
	@Test
	void logInfo() {
		when(realmProvider.getRealmsStream()).thenReturn(Stream.of());
		task(Duration.ofMillis(300), Duration.ZERO, Duration.ofMillis(200)).run(session);
		assertLog(Level.INFO, "Finished scrapping keycloak stats in {0}.");
	}

	@DisplayName("log - warn")
	@Test
	void logWarn() {
		when(realmProvider.getRealmsStream()).thenReturn(Stream.of());
		task(Duration.ofMillis(300), Duration.ofMillis(100), Duration.ZERO).run(session);
		assertLog(Level.WARNING, "Finished scrapping keycloak stats in {0}, consider to increase interval.");
	}

	@DisplayName("log - error")
	@Test
	void logError() {
		when(realmProvider.getRealmsStream()).thenReturn(Stream.of());
		task(Duration.ZERO, Duration.ofMillis(100), Duration.ofMillis(200)).run(session);
		assertLog(Level.SEVERE, "Finished scrapping keycloak stats in {0}, consider to increase interval.");
	}

	@DisplayName("scrape")
	@Test
	void scrape() {

		var realm = UUID.randomUUID().toString();
		var realmModel = mock(RealmModel.class);
		var client1 = UUID.randomUUID().toString();
		var client1Id = UUID.randomUUID().toString();
		var client1Model = mock(ClientModel.class);
		var client2 = UUID.randomUUID().toString();
		var client2Id = UUID.randomUUID().toString();
		var client2Model = mock(ClientModel.class);
		when(realmModel.getName()).thenReturn(realm);
		when(realmModel.getClientsStream()).then(i -> Stream.of(client1Model, client2Model));
		when(client1Model.getId()).thenReturn(client1Id);
		when(client1Model.getClientId()).thenReturn(client1);
		when(client2Model.getId()).thenReturn(client2Id);
		when(client2Model.getClientId()).thenReturn(client2);

		when(session.clients()).thenReturn(clientProvider);
		when(session.users()).thenReturn(userProvider);
		when(session.sessions()).thenReturn(sessionProvider);
		when(realmProvider.getRealmsStream()).then(i -> Stream.of(realmModel));

		// empty realm

		when(userProvider.getUsersCount(realmModel)).thenReturn(0);
		when(clientProvider.getClientsCount(realmModel)).thenReturn(0L);
		when(sessionProvider.getOfflineSessionsCount(realmModel, client1Model)).thenReturn(0L);
		when(sessionProvider.getOfflineSessionsCount(realmModel, client2Model)).thenReturn(0L);
		when(sessionProvider.getActiveUserSessions(realmModel, client1Model)).thenReturn(0L);
		when(sessionProvider.getActiveUserSessions(realmModel, client2Model)).thenReturn(0L);
		when(sessionProvider.getActiveClientSessionStats(realmModel, false)).thenReturn(Map.of());
		task().run(session);
		assertUsersCount(realmModel, 0);
		assertClientsCount(realmModel, 0);
		assertOfflineSessions(realmModel, client1Model, 0);
		assertOfflineSessions(realmModel, client2Model, 0);
		assertActiveUserSessions(realmModel, client1Model, 0);
		assertActiveUserSessions(realmModel, client2Model, 0);
		assertActiveClientSessions(realmModel, client1Model, 0);
		assertActiveClientSessions(realmModel, client2Model, 0);

		// initial values

		when(userProvider.getUsersCount(realmModel)).thenReturn(10);
		when(clientProvider.getClientsCount(realmModel)).thenReturn(20L);
		when(sessionProvider.getOfflineSessionsCount(realmModel, client1Model)).thenReturn(0L);
		when(sessionProvider.getOfflineSessionsCount(realmModel, client2Model)).thenReturn(1L);
		when(sessionProvider.getActiveUserSessions(realmModel, client1Model)).thenReturn(2L);
		when(sessionProvider.getActiveUserSessions(realmModel, client2Model)).thenReturn(3L);
		when(sessionProvider.getActiveClientSessionStats(realmModel, false))
				.thenReturn(Map.of(client1Id, 5L, client2Id, 0L));
		task().run(session);
		assertUsersCount(realmModel, 10);
		assertClientsCount(realmModel, 20);
		assertOfflineSessions(realmModel, client1Model, 0);
		assertOfflineSessions(realmModel, client2Model, 1);
		assertActiveUserSessions(realmModel, client1Model, 2);
		assertActiveUserSessions(realmModel, client2Model, 3);
		assertActiveClientSessions(realmModel, client1Model, 5);
		assertActiveClientSessions(realmModel, client2Model, 0);

		// updated values

		when(userProvider.getUsersCount(realmModel)).thenReturn(11);
		when(clientProvider.getClientsCount(realmModel)).thenReturn(19L);
		when(sessionProvider.getOfflineSessionsCount(realmModel, client1Model)).thenReturn(3L);
		when(sessionProvider.getOfflineSessionsCount(realmModel, client2Model)).thenReturn(2L);
		when(sessionProvider.getActiveUserSessions(realmModel, client1Model)).thenReturn(1L);
		when(sessionProvider.getActiveUserSessions(realmModel, client2Model)).thenReturn(0L);
		when(sessionProvider.getActiveClientSessionStats(realmModel, false))
				.thenReturn(Map.of(client1Id, 4L, client2Id, 3L));
		task().run(session);
		assertUsersCount(realmModel, 11);
		assertClientsCount(realmModel, 19);
		assertOfflineSessions(realmModel, client1Model, 3);
		assertOfflineSessions(realmModel, client2Model, 2);
		assertActiveUserSessions(realmModel, client1Model, 1);
		assertActiveUserSessions(realmModel, client2Model, 0);
		assertActiveClientSessions(realmModel, client1Model, 4);
		assertActiveClientSessions(realmModel, client2Model, 3);
	}

	private MetricsStatsTask task() {
		return task(Duration.ofMillis(300), Duration.ofMillis(100), Duration.ofMillis(200));
	}

	private MetricsStatsTask task(Duration interval, Duration infoThreshold, Duration warnThreshold) {
		return new MetricsStatsTask(interval, infoThreshold, warnThreshold);
	}

	private static void assertUsersCount(RealmModel realm, int count) {
		assertGauge("keycloak_users", realm, null, count);
	}

	private static void assertClientsCount(RealmModel realm, int count) {
		assertGauge("keycloak_clients", realm, null, count);
	}

	private static void assertActiveClientSessions(RealmModel realm, ClientModel client, int count) {
		assertGauge("keycloak_active_client_sessions", realm, client, count);
	}

	private static void assertActiveUserSessions(RealmModel realm, ClientModel client, int count) {
		assertGauge("keycloak_active_user_sessions", realm, client, count);
	}

	private static void assertOfflineSessions(RealmModel realm, ClientModel client, int count) {
		assertGauge("keycloak_offline_sessions", realm, client, count);
	}

	private static void assertGauge(String name, RealmModel realm, ClientModel client, int count) {
		var gauges = Metrics.globalRegistry.getMeters().stream()
				.filter(Gauge.class::isInstance)
				.filter(gauge -> gauge.getId().getName().equals(name))
				.filter(gauge -> gauge.getId().getTag("realm").equals(realm.getName()))
				.filter(gauge -> client == null || gauge.getId().getTag("client").equals(client.getClientId()))
				.map(Gauge.class::cast)
				.toList();
		assertEquals(1, gauges.size());
		assertEquals(count, gauges.get(0).value());
	}
}
