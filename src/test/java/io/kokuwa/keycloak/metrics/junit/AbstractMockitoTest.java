package io.kokuwa.keycloak.metrics.junit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestClassOrder;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Mockito base class with configured logging.
 *
 * @author Stephan Schnabel
 */
@ExtendWith(MockitoExtension.class)
@TestClassOrder(ClassOrderer.DisplayName.class)
@TestMethodOrder(MethodOrderer.DisplayName.class)
public abstract class AbstractMockitoTest {

	private static final List<LogRecord> LOGS = new ArrayList<>();

	static {

		System.setProperty("org.jboss.logging.provider", "jdk");
		System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tT %4$-5s %2$s %5$s%6$s%n");

		Logger.getLogger("org.junit").setLevel(Level.INFO);
		Logger.getLogger("").setLevel(Level.ALL);
		Logger.getLogger("").addHandler(new Handler() {

			@Override
			public void publish(LogRecord log) {
				LOGS.add(log);
			}

			@Override
			public void flush() {}

			@Override
			public void close() {}
		});
	}

	@BeforeEach
	void reset() {
		Metrics.globalRegistry.clear();
		Metrics.addRegistry(new SimpleMeterRegistry());
		LOGS.clear();
	}

	public static void assertLog(Level level, String message) {
		assertTrue(LOGS.stream()
				.filter(l -> l.getLevel().equals(level))
				.filter(l -> l.getMessage().equals(message))
				.findAny().isPresent(),
				"log with level " + level + " and message " + message + " not found");
	}
}
