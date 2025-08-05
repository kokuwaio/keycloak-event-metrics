FROM maven:3.9.11-eclipse-temurin-17@sha256:d404443621d2b79c52d50c05ad9e18a401e9030e06c6c80e2b5707b7d9987db1 AS build
SHELL ["/usr/bin/bash", "-e", "-u", "-c"]
WORKDIR /build
ARG MAVEN_ARGS="--batch-mode --color=always --no-transfer-progress"
ARG MAVEN_MIRROR_CENTRAL
RUN mkdir "$HOME/.m2" && printf "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\
<settings>\n\
	<localRepository>/tmp/mvn-repo</localRepository>\n\
	<mirrors><mirror><url>%s</url><mirrorOf>central</mirrorOf></mirror></mirrors>\n\
</settings>" "${MAVEN_MIRROR_CENTRAL:-https://repo.maven.apache.org/maven2}" > "$HOME/.m2/settings.xml"
COPY . .
RUN --mount=type=cache,target=/tmp/mvn-repo mvn package -DskipTests -P=-dev

FROM busybox:1.37.0-uclibc@sha256:6c52233239cf6680234f248120e2696de7af93e9d636a08ee762eb2e99ae26a3
COPY --from=build --chmod=444 /build/target/keycloak-event-metrics.jar /opt/keycloak/providers/keycloak-event-metrics.jar
