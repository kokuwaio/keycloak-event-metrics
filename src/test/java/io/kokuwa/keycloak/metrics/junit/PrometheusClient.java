package io.kokuwa.keycloak.metrics.junit;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

/**
 * JAX-RS client for prometheus endpoint.
 *
 * @author Stephan Schnabel
 */
public interface PrometheusClient {

	@GET
	@Path("/metrics")
	@Consumes(MediaType.TEXT_PLAIN)
	String scrap();
}
