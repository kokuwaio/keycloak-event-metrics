package io.kokuwa.keycloak.metrics.prometheus;

import java.util.Map;

/**
 * Represents a parsed Prometheus line.
 *
 * @author Stephan Schnabel
 * @param name  Metric name
 * @param tags  Tags for this metriv value
 * @param value Metric value
 */
public record PrometheusMetric(String name, Map<String, String> tags, Double value) {}
