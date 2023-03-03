package io.kokuwa.keycloak.metrics;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

/**
 * Listener for {@link Event} and {@link AdminEvent}.
 *
 * @author Stephan Schnabel
 */
public class MicrometerEventListener implements EventListenerProvider, AutoCloseable {

	private final MicrometerEventRecorder recorder;

	MicrometerEventListener(MicrometerEventRecorder recorder) {
		this.recorder = recorder;
	}

	@Override
	public void onEvent(Event event) {
		recorder.userEvent(event);
	}

	@Override
	public void onEvent(AdminEvent event, boolean includeRepresentation) {
		recorder.adminEvent(event);
	}

	@Override
	public void close() {}
}
