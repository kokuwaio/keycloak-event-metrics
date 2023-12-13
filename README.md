# Keycloak Metrics

Provides metrics for Keycloak user/admin events and user/client/session count. Tested on Keycloak [21-23](.github/workflows/pr.yaml#L50-L53).

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/kokuwaio/keycloak-event-metrics.svg?label=License)](http://www.apache.org/licenses/)
[![Maven Central](https://img.shields.io/maven-central/v/io.kokuwa.keycloak/keycloak-event-metrics.svg?label=Maven%20Central)](https://central.sonatype.com/search?namespace=io.kokuwa.keycloak&q=keycloak-event-metrics)
[![Build](https://img.shields.io/github/actions/workflow/status/kokuwaio/keycloak-event-metrics/build.yaml?label=Build)](https://github.com/kokuwaio/keycloak-event-metrics/actions/workflows/build.yaml)

## Why?

[aerogear/keycloak-metrics-spi](https://github.com/aerogear/keycloak-metrics-spi) is an alternative to this plugin but is not well maintained. This implementation is different:

* no Prometheus push (event listener only adds counter to Micrometer)
* no realm specific Prometheus endpoint, only `/metrics` (from Quarkus)
* no jvm/http metrics, this is [already](https://www.keycloak.org/server/configuration-metrics#_available_metrics) included in Keycloak
* different metric names, can relace model ids with name (see [configuration](#kc_metrics_event_replace_ids))
* deployed to maven central and very small (15 kb vs. 151 KB [aerogear/keycloak-metrics-spi](https://github.com/aerogear/keycloak-metrics-spi))
* gauge for active/offline sessions and user/client count

## What?

Resuses micrometer from Quarkus distribution to add metrics for Keycloak for events.

### User Events

User events are added with key `keycloak_event_user_total` and tags:

* `type`: [EventType](https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/EventType.java#L27) from [Event#type](https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/Event.java#L44)
* `realm`: realm id from [Event#realmId](https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/Event.java#L46)
* `client`: client id from [Event#clientId](https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/Event.java#L48)
* `error`: error from [Event#error](https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/Event.java#L56), only present for error types

Examples:

```txt
keycloak_event_user_total{client="test",realm="9039a0b5-e8c9-437a-a02e-9d91b04548a4",type="LOGIN",error="",} 2.0
keycloak_event_user_total{client="test",realm="1fdb3465-1675-49e8-88ad-292e2f42ee72",type="LOGIN",error="",} 1.0
keycloak_event_user_total{client="test",realm="1fdb3465-1675-49e8-88ad-292e2f42ee72",type="LOGIN_ERROR",error="invalid_user_credentials",} 1.0
```

### Admin Events

Admin events are added with key `keycloak_event_admin_total` and tags:

* `realm`: realm id from [AdminEvent#realmId](https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/admin/AdminEvent.java#L44)
* `operation`: [OperationType](https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/admin/OperationType.java#L27) from [AdminEvent#operationType](https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/admin/AdminEvent.java#L53)
* `resource`: [ResourceType](https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/admin/ResourceType.java#L24) from [AdminEvent#resourceType](https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/admin/AdminEvent.java#L51)
* `error`: error from [AdminEvent#error](https://github.com/keycloak/keycloak/blob/main/server-spi-private/src/main/java/org/keycloak/events/admin/AdminEvent.java#L59), only present for error types

Examples:

```txt
keycloak_event_admin_total{error="",operation="CREATE",realm="1fdb3465-1675-49e8-88ad-292e2f42ee72",resource="USER",} 1.0
keycloak_event_admin_total{error="",operation="CREATE",realm="9039a0b5-e8c9-437a-a02e-9d91b04548a4",resource="USER",} 1.0
```

## Configuration

### `KC_METRICS_EVENT_REPLACE_IDS`

Set to `true` (the default value) than replace model ids from events with names:

* [RealmModel#getId()](https://github.com/keycloak/keycloak/blob/main/server-spi/src/main/java/org/keycloak/models/RealmModel.java#L82) with [RealmModel#getName()](https://github.com/keycloak/keycloak/blob/main/server-spi/src/main/java/org/keycloak/models/RealmModel.java#L84)

Metrics:

```txt
keycloak_event_user_total{client="test-client",error="",realm="test-realm",type="LOGIN",} 2.0
keycloak_event_user_total{client="other-client",error="",realm="other-realm",type="LOGIN",} 1.0
keycloak_event_user_total{client="other-client",error="invalid_user_credentials",realm="other-realm",type="LOGIN_ERROR",} 1.0
```

### `KC_METRICS_STATS_ENABLED`

Set to `true` (default is `false`) to provide metrics for user/client count per realm and session count per client. Metrics:

```txt
# HELP keycloak_users  
# TYPE keycloak_users gauge
keycloak_users{realm="master",} 1.0
keycloak_users{realm="my-realm",} 2.0
keycloak_users{realm="other-realm",} 1.0# HELP keycloak_active_user_sessions
# TYPE keycloak_active_user_sessions gauge
keycloak_active_user_sessions{client="admin-cli",realm="userCount_1",} 0.0
keycloak_active_user_sessions{client="admin-cli",realm="userCount_2",} 0.0
keycloak_active_user_sessions{client="admin-cli",realm="master",} 1.0
# TYPE keycloak_active_client_sessions gauge
keycloak_active_client_sessions{client="admin-cli",realm="userCount_1",} 0.0
keycloak_active_client_sessions{client="admin-cli",realm="userCount_2",} 0.0
keycloak_active_client_sessions{client="admin-cli",realm="master",} 0.0
# TYPE keycloak_offline_sessions gauge
keycloak_offline_sessions{client="admin-cli",realm="userCount_1",} 0.0
keycloak_offline_sessions{client="admin-cli",realm="userCount_2",} 0.0
keycloak_offline_sessions{client="admin-cli",realm="master",} 0.0
```

### `KC_METRICS_STATS_INTERVAL`

If `KC_METRICS_STATS_ENABLED` is `true` this will define the interval for scraping. If not configured `PT60s` will be used.

### `KC_METRICS_STATS_INFO_THRESHOLD` and `KC_METRICS_STATS_WARN_THRESHOLD`

If `KC_METRICS_STATS_ENABLED` is `true` this envs will define logging if scraping takes to long. Both envs are parsed as `java.lang.Duration`.

Default values:

* `KC_METRICS_STATS_INFO_THRESHOLD`: 50% of `KC_METRICS_STATS_INTERVAL` = 30s
* `KC_METRICS_STATS_WARN_THRESHOLD`: 75% of `KC_METRICS_STATS_INTERVAL` = 45s

If scrapping takes less than `KC_METRICS_STATS_INFO_THRESHOLD` duration will be logged on debug level.

## Installation

### Grafana Dashboard

Can be found here: [kokuwaio/keycloak keycloak-metrics.json](https://github.com/kokuwaio/keycloak/blob/main/src/test/k3s/dev/grafana/files/dashboards/keycloak-metrics.json)

### Testcontainers

For usage in [Testcontainers](https://www.testcontainers.org/) see [KeycloakExtension.java](src/test/java/io/kokuwa/keycloak/metrics/junit/KeycloakExtension.java#L57-L68)

### Docker

Check: [kokuwaio/keycloak](https://github.com/kokuwaio/keycloak)

Dockerfile:

```Dockerfile
FROM quay.io/keycloak/keycloak:21.0.1

ENV KEYCLOAK_ADMIN=admin
ENV KEYCLOAK_ADMIN_PASSWORD=password
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_LOG_CONSOLE_COLOR=true

ADD target/keycloak-event-metrics-0.0.1-SNAPSHOT.jar /opt/keycloak/providers
RUN /opt/keycloak/bin/kc.sh build
```

Run:

```sh
docker build . --tag keycloak:metrics
docker run --rm -p8080 keycloak:metrics start-dev
```
