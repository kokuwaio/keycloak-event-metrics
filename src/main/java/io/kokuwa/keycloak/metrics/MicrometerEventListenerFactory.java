package io.kokuwa.keycloak.metrics;

import com.google.common.collect.ImmutableList;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import javax.enterprise.inject.spi.CDI;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.timer.TimerProvider;

/**
 * Factory for {@link MicrometerEventListener}, uses {@link MeterRegistry} from CDI.
 *
 * @author Stephan Schnabel
 */
public class MicrometerEventListenerFactory implements EventListenerProviderFactory {

  private static final String PROVIDER_ID = "metrics-listener";
  private static final int INTERVAL = 60 * 1000; //1 MINUTE
	private static final Logger log = Logger.getLogger(MicrometerEventListener.class);
	private MeterRegistry registry;
	private boolean replace;

	@Override
	public String getId() {
		return PROVIDER_ID;
	}

	@Override
	public void init(Scope config) {
		replace = "true".equals(System.getenv("KC_METRICS_EVENT_REPLACE_IDS"));
		log.info(replace ? "Configured with model names." : "Configured with model ids.");
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		registry = CDI.current().select(MeterRegistry.class).get();

                KeycloakModelUtils.runJobInTransaction(factory, s1 -> {
                    TimerProvider timer = s1.getProvider(TimerProvider.class);
                    log.info("Registering gauge update job TimerProvider");
                    timer.schedule(() -> {
                        KeycloakModelUtils.runJobInTransaction(s1.getKeycloakSessionFactory(), s2 -> {
                            log.info("Updating gauges");
                            updateGauges(s2);
                          });
                      }, INTERVAL, PROVIDER_ID);
                  });
	}

	@Override
	public EventListenerProvider create(KeycloakSession session) {
		return new MicrometerEventListener(registry, session, replace);
	}

	@Override
	public void close() {}


  private Iterable<Tag> tags(String... tags) {
    if (tags.length % 2 != 0) throw new IllegalStateException("Tag name value pairs must be even"); 
    ImmutableList.Builder<Tag> builder = new ImmutableList.Builder<Tag>();
    for (int i = 0;i < tags.length;i+=2) {
      builder.add(new ImmutableTag(tags[i], tags[i+1]));
    }
    return builder.build();
  }
    
  private void updateGauges(KeycloakSession session) {
    session.realms().getRealmsStream().forEach(realm -> {
        // keycloak_users = total number of users
        registry.gauge("keycloak_users",
                       tags("realm", replace ? realm.getName() : realm.getId()),
                       session.users().getUsersCount(realm));
      
        // keycloak_clients = total number of clients
        registry.gauge("keycloak_clients",
                       tags("realm", replace ? realm.getName() : realm.getId()),
                       realm.getClientsCount());

        // sessions - by client

        realm.getClientsStream().forEach(client -> {
            // keycloak_active_user_sessions
            registry.gauge("keycloak_active_user_sessions",
                           tags("realm", replace ? realm.getName() : realm.getId(),
                                "client", replace ? client.getClientId() : client.getId()),
                           session.sessions().getActiveUserSessions(realm, client));
            // keycloak_active_client_sessions
            registry.gauge("keycloak_active_client_sessions",
                           tags("realm", replace ? realm.getName() : realm.getId(),
                                "client", replace ? client.getClientId() : client.getId()),
                           session.sessions().getActiveClientSessionStats(realm,false).get(client.getId()));

            // keycloak_offline_sessions
            registry.gauge("keycloak_offline_sessions",
                           tags("realm", replace ? realm.getName() : realm.getId(),
                                "client", replace ? client.getClientId() : client.getId()),
                           session.sessions().getOfflineSessionsCount(realm, client));
          });
      });    
  }
}
