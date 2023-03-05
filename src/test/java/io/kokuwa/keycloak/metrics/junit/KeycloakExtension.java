package io.kokuwa.keycloak.metrics.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Duration;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.client.ClientBuilder;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.token.TokenService;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import io.kokuwa.keycloak.metrics.prometheus.Prometheus;
import io.kokuwa.keycloak.metrics.prometheus.PrometheusClient;

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
		var jar = properties.getProperty("jar");
		var timeout = properties.getProperty("timeout");

		// create and start container

		@SuppressWarnings("resource")
		var container = new GenericContainer<>("quay.io/keycloak/keycloak:" + version)
				.withEnv("KEYCLOAK_ADMIN", "admin")
				.withEnv("KEYCLOAK_ADMIN_PASSWORD", "password")
				.withEnv("KC_LOG_CONSOLE_COLOR", "true")
				.withEnv("KC_HEALTH_ENABLED", "true")
				.withEnv("KC_METRICS_ENABLED", "true")
				.withEnv("KC_METRICS_EVENT_REPLACE_IDS", "true")
				.withCopyFileToContainer(MountableFile.forHostPath(jar), "/opt/keycloak/providers/metrics.jar")
				.withLogConsumer(out -> System.out.print(out.getUtf8String()))
				.withExposedPorts(8080)
				.withStartupTimeout(Duration.parse(timeout))
				.waitingFor(Wait.forHttp("/health").forPort(8080))
				.withCommand("start-dev");
		try {
			container.start();
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
		client = new KeycloakClient(keycloak, token);
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
