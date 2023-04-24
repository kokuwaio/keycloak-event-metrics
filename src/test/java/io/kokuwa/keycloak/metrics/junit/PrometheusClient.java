package io.kokuwa.keycloak.metrics.junit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

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
