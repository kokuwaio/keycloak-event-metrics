package io.kokuwa.keycloak.metrics.junit;

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

	@BeforeEach
	void reset() {
		Metrics.globalRegistry.clear();
		Metrics.addRegistry(new SimpleMeterRegistry());
	}
}
