package io.kokuwa.keycloak.metrics.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.Set;

import jakarta.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.token.TokenService;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * JUnit extension to start keycloak.
 *
 * @author Stephan Schnabel
 */
public class KeycloakExtension implements BeforeAllCallback, ParameterResolver {

	private static KeycloakClient client;
	private static Prometheus prometheus;

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {

		if (client != null) {
			return;
		}

		// get test properties

		var properties = new Properties();
		try {
			properties.load(getClass().getResourceAsStream("/test.properties"));
		} catch (IOException e) {
			throw new Exception("Failed to read properties", e);
		}
		var version = properties.getProperty("version");
		var image = "quay.io/keycloak/keycloak:" + version;
		var jar = properties.getProperty("jar");
		var timeout = properties.getProperty("timeout");

		// create and start container - use fixed port in ci

		@SuppressWarnings("deprecation")
		var container = (System.getenv("CI") == null
				? new GenericContainer<>(image).withExposedPorts(8080)
				: new FixedHostPortGenericContainer<>(image).withFixedExposedPort(8080, 8080));
		try {
			container
					.withEnv("KEYCLOAK_ADMIN", "admin")
					.withEnv("KEYCLOAK_ADMIN_PASSWORD", "password")
					.withEnv("KC_LOG_LEVEL", "io.kokuwa:trace")
					// otherwise port 9000 will be used, with this config we can test different keycloak versions
					.withEnv("KC_LEGACY_OBSERVABILITY_INTERFACE", "true")
					.withEnv("KC_HEALTH_ENABLED", "true")
					.withEnv("KC_METRICS_ENABLED", "true")
					.withEnv("KC_METRICS_STATS_ENABLED", "true")
					.withEnv("KC_METRICS_STATS_INTERVAL", "PT1s")
					.withCopyFileToContainer(MountableFile.forHostPath(jar), "/opt/keycloak/providers/metrics.jar")
					.withLogConsumer(out -> System.out.print(out.getUtf8String()))
					.withStartupTimeout(Duration.parse(timeout))
					.waitingFor(Wait.forHttp("/health").forPort(8080).withStartupTimeout(Duration.ofMinutes(10)))
					.withCommand("start-dev")
					.start();
		} catch (RuntimeException e) {
			throw new Exception("Failed to start keycloak", e);
		}

		// create client for keycloak container

		var url = "http://" + container.getHost() + ":" + container.getMappedPort(8080);
		var keycloak = Keycloak.getInstance(url, "master", "admin", "password", "admin-cli");
		assertEquals(version, keycloak.serverInfo().getInfo().getSystemInfo().getVersion(), "version invalid");
		var target = ClientBuilder.newClient().target(url);
		var token = Keycloak.getClientProvider().targetProxy(target, TokenService.class);
		prometheus = new Prometheus(Keycloak.getClientProvider().targetProxy(target, PrometheusClient.class));
		client = new KeycloakClient(url, keycloak, token);
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return Set.of(KeycloakClient.class, Prometheus.class).contains(parameterContext.getParameter().getType());
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
		return parameterContext.getParameter().getType().equals(KeycloakClient.class) ? client : prometheus;
	}
}
