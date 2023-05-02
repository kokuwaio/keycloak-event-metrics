package io.kokuwa.keycloak.metrics.stats;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.ReflectionUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakTransactionManager;
import org.keycloak.timer.TimerProvider;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Spy;

import io.kokuwa.keycloak.metrics.junit.AbstractMockitoTest;

/**
 * Test for {@link MetricsStatsFactory} with Mockito.
 *
 * @author Stephan Schnabel
 */
@DisplayName("metrics: factory")
public class MetricsStatsFactoryTest extends AbstractMockitoTest {

	@Spy
	MetricsStatsFactoryImpl factory;
	@Mock
	KeycloakSessionFactory sessionFactory;
	@Mock
	KeycloakSession session;

	@DisplayName("disabled")
	@Test
	void disabled() {
		factory.init(null);
		factory.postInit(sessionFactory);
		assertNull(factory.create(session));
		factory.close();
	}

	@DisplayName("enabled - with default values")
	@Test
	void enabledDefault() {
		when(factory.getenv("KC_METRICS_STATS_ENABLED")).thenReturn("true");
		when(factory.getenv("KC_METRICS_STATS_INTERVAL")).thenReturn(null);
		when(factory.getenv("KC_METRICS_STATS_INFO_THRESHOLD")).thenReturn(null);
		when(factory.getenv("KC_METRICS_STATS_WARN_THRESHOLD")).thenReturn(null);
		assertTask(Duration.ofSeconds(60), Duration.ofSeconds(30), Duration.ofSeconds(45));
	}

	@DisplayName("enabled - with custom interval")
	@Test
	void enabledCustomInterval() {
		when(factory.getenv("KC_METRICS_STATS_ENABLED")).thenReturn("true");
		when(factory.getenv("KC_METRICS_STATS_INTERVAL")).thenReturn("PT300s");
		when(factory.getenv("KC_METRICS_STATS_INFO_THRESHOLD")).thenReturn(null);
		when(factory.getenv("KC_METRICS_STATS_WARN_THRESHOLD")).thenReturn(null);
		assertTask(Duration.ofSeconds(300), Duration.ofSeconds(150), Duration.ofSeconds(225));
	}

	@DisplayName("enabled - with custom thresholds")
	@Test
	void enabledCustomThresholds() {
		when(factory.getenv("KC_METRICS_STATS_ENABLED")).thenReturn("true");
		when(factory.getenv("KC_METRICS_STATS_INTERVAL")).thenReturn(null);
		when(factory.getenv("KC_METRICS_STATS_INFO_THRESHOLD")).thenReturn("PT40s");
		when(factory.getenv("KC_METRICS_STATS_WARN_THRESHOLD")).thenReturn("PT50s");
		assertTask(Duration.ofSeconds(60), Duration.ofSeconds(40), Duration.ofSeconds(50));
	}

	private void assertTask(Duration interval, Duration infoThreshold, Duration warnThreshold) {

		var timerProvider = mock(TimerProvider.class);
		when(sessionFactory.create()).thenReturn(session);
		when(session.getProvider(TimerProvider.class)).thenReturn(timerProvider);
		when(session.getTransactionManager()).thenReturn(mock(KeycloakTransactionManager.class));

		factory.postInit(sessionFactory);

		var taskCaptor = ArgumentCaptor.forClass(MetricsStatsTask.class);
		verify(timerProvider).scheduleTask(
				taskCaptor.capture(),
				ArgumentMatchers.eq(interval.toMillis()),
				ArgumentMatchers.eq("metrics"));
		assertNotNull(taskCaptor.getValue(), "task");
		assertField(interval, taskCaptor.getValue(), "interval");
		assertField(infoThreshold, taskCaptor.getValue(), "infoThreshold");
		assertField(warnThreshold, taskCaptor.getValue(), "warnThreshold");
	}

	private void assertField(Duration expected, MetricsStatsTask task, String name) {
		assertEquals(
				expected,
				assertDoesNotThrow(() -> ReflectionUtils.tryToReadFieldValue(MetricsStatsTask.class, name, task).get()),
				"field " + name + " invalid");
	}
}
