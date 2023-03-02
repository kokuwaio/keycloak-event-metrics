package io.kokuwa.keycloak.metrics;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Micrometer based recorder for events.
 *
 * @author Stephan Schnabel
 */
public class MicrometerEventRecorder {

	private static final String PREFIX = "keycloak_";
	private static final String USER_EVENT_PREFIX = PREFIX + "user_event_";
	private static final String ADMIN_EVENT_PREFIX = PREFIX + "admin_event_";

	private static final String LOGIN_ATTEMPTS = PREFIX + "login_attempts";
	private static final String LOGIN_SUCCESS = PREFIX + "logins";
	private static final String LOGIN_FAILURE = PREFIX + "failed_login_attempts";
	private static final String CLIENT_LOGIN_SUCCESS = PREFIX + "client_logins";
	private static final String CLIENT_LOGIN_FAILURE = PREFIX + "failed_client_login_attempts";
	private static final String REGISTER_SUCCESS = PREFIX + "registrations";
	private static final String REGISTER_FAILURE = PREFIX + "registrations_errors";
	private static final String REFRESH_TOKEN_SUCCESS = PREFIX + "refresh_tokens";
	private static final String REFRESH_TOKEN_FAILURE = PREFIX + "refresh_tokens_errors";
	private static final String CODE_TO_TOKEN_SUCCESS = PREFIX + "code_to_tokens";
	private static final String CODE_TO_TOKEN_FAILURE = PREFIX + "code_to_tokens_errors";

	private final Map<String, Counter> counters = new HashMap<>();
	private final MeterRegistry registry;

	MicrometerEventRecorder(MeterRegistry registry) {
		this.registry = registry;
	}

	void adminEvent(AdminEvent event) {
		counter(ADMIN_EVENT_PREFIX + event.getOperationType().name(),
				"realm", event.getRealmId(),
				"resource", event.getResourceType() == null ? "" : event.getResourceType().name());
	}

	void userEvent(Event event) {

		var tags = new String[] {
				"provider", Optional
					.ofNullable(event.getDetails()).orElseGet(Map::of)
					.getOrDefault("identity_provider", "keycloak"), 
				"realm", event.getRealmId() == null ? "" : event.getRealmId(), 
				"client_id", event.getClientId() == null ? "" : event.getClientId(),
				"error", event.getError() == null ? "" : event.getError() };

		switch (event.getType()) {
			case LOGIN:
				counter(LOGIN_ATTEMPTS, tags);
				counter(LOGIN_SUCCESS, tags);
				break;
			case LOGIN_ERROR:
				counter(LOGIN_ATTEMPTS, tags);
				counter(LOGIN_FAILURE, tags);
				break;
			case CLIENT_LOGIN:
				counter(CLIENT_LOGIN_SUCCESS, tags);
				break;
			case CLIENT_LOGIN_ERROR:
				counter(CLIENT_LOGIN_FAILURE, tags);
				break;
			case REGISTER:
				counter(REGISTER_SUCCESS, tags);
				break;
			case REGISTER_ERROR:
				counter(REGISTER_FAILURE, tags);
				break;
			case REFRESH_TOKEN:
				counter(REFRESH_TOKEN_SUCCESS, tags);
				break;
			case REFRESH_TOKEN_ERROR:
				counter(REFRESH_TOKEN_FAILURE, tags);
				break;
			case CODE_TO_TOKEN:
				counter(CODE_TO_TOKEN_SUCCESS, tags);
				break;
			case CODE_TO_TOKEN_ERROR:
				counter(CODE_TO_TOKEN_FAILURE, tags);
				break;
			default:
				counter(USER_EVENT_PREFIX + event.getType().name(), tags);
		}
	}

	private void counter(String counter, String... tags) {
		counters.computeIfAbsent(counter + Arrays.toString(tags), string -> registry.counter(counter, tags))
				.increment();
	}
}
