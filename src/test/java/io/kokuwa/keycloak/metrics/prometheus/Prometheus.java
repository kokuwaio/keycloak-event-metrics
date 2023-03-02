package io.kokuwa.keycloak.metrics.prometheus;

import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Client to access Prometheus metric values:
 *
 * @author Stephan Schnabel
 */
public class Prometheus {

	private final Set<PrometheusMetric> state = new HashSet<>();
	private final PrometheusClient client;

	public Prometheus(PrometheusClient client) {
		this.client = client;
	}

	public int loginAttempts() {
		return scrap("keycloak_login_attempts_total").intValue();
	}

	public int loginAttempts(String realmName) {
		return scrap("keycloak_login_attempts_total", "realm", realmName).intValue();
	}

	public int loginSuccess() {
		return scrap("keycloak_logins_total").intValue();
	}

	public int loginSuccess(String realmName) {
		return scrap("keycloak_logins_total", "realm", realmName).intValue();
	}

	public int loginFailure() {
		return scrap("keycloak_failed_login_attempts_total").intValue();
	}

	public int loginFailure(String realmName) {
		return scrap("keycloak_failed_login_attempts_total", "realm", realmName).intValue();
	}

	public void scrap() {
		state.clear();
		Stream.of(client.scrap().split("[\\r\\n]+"))
				.filter(line -> !line.startsWith("#"))
				.filter(line -> line.startsWith("keycloak"))
				.map(line -> {
					var name = line.substring(0, line.contains("{") ? line.indexOf("{") : line.lastIndexOf(" "));
					var tags = line.contains("{")
							? Stream.of(line.substring(line.indexOf("{") + 1, line.indexOf("}")).split(","))
									.map(tag -> tag.split("="))
									.filter(tag -> tag.length >= 2)
									.collect(Collectors.toMap(tag -> tag[0], tag -> tag[1].replace("\"", "")))
							: Map.<String, String>of();
					var value = Double.parseDouble(line.substring(line.lastIndexOf(" ")));
					return new PrometheusMetric(name, tags, value);
				})
				.forEach(state::add);
	}

	private Double scrap(String name) {
		return state.stream()
				.filter(metric -> Objects.equals(metric.name(), name))
				.mapToDouble(PrometheusMetric::value)
				.sum();
	}

	private Double scrap(String name, String tag, String value) {
		return state.stream()
				.filter(metric -> Objects.equals(metric.name(), name))
				.filter(metric -> Objects.equals(metric.tags().get(tag), value))
				.mapToDouble(PrometheusMetric::value)
				.sum();
	}
}
