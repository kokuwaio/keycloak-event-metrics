package io.kokuwa.keycloak.metrics.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ServiceLoader;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.kokuwa.keycloak.metrics.junit.AbstractMockitoTest;

/**
 * Test for {@link MetricsStatsSpi} with Mockito.
 *
 * @author Stephan Schnabel
 */
@DisplayName("metrics: spi")
public class MetricsStatsSpiTest extends AbstractMockitoTest {

	@Test
	void test() {

		var spi = new MetricsStatsSpi();
		assertEquals("metrics", spi.getName(), "getName()");
		assertFalse(spi.isInternal(), "isInternal()");
		assertNotNull(spi.getProviderClass(), "getProviderClass()");
		assertTrue(spi.getProviderFactoryClass().isInterface(), "getProviderFactoryClass() - should be an interface");

		var factory = ServiceLoader.load(spi.getProviderFactoryClass()).findFirst().orElse(null);
		assertNotNull(factory, "failed to read factory with service loader");
		assertEquals(MetricsStatsFactoryImpl.class, factory.getClass(), "factory.class");
		assertEquals("default", factory.getId(), "factory.id");
	}
}
