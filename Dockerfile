FROM maven:3.9.11-eclipse-temurin-17@sha256:4ce75883ff3a374566eb9d438fa6723ec0e6bdef2436e4c5255f9cd4a3125fe6 AS build
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

FROM busybox:1.37.0-uclibc@sha256:7b5ade1b11f52517f5cdf3b6194588587b9c4b14d3244b6c96ed7f5b0ff31458
COPY --from=build --chmod=444 /build/target/keycloak-event-metrics.jar /opt/keycloak/providers/keycloak-event-metrics.jar
