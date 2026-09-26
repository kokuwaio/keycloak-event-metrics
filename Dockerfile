FROM maven:3.9.16-eclipse-temurin-17@sha256:d764acd5a379cd522b84eb40b5cfc0f3a0bdef71f5f3ff58db0d2c6a54af02ec AS build
SHELL ["/usr/bin/bash", "-e", "-u", "-c"]
WORKDIR /build
ARG MAVEN_ARGS="--batch-mode --color=always --no-transfer-progress"
ARG MAVEN_MIRROR_URL
RUN mkdir "$HOME/.m2" && printf "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\
<settings>\n\
	<localRepository>/tmp/mvn-repo</localRepository>\n\
	<mirrors><mirror><url>%s</url><mirrorOf>central</mirrorOf></mirror></mirrors>\n\
</settings>" "${MAVEN_MIRROR_URL:-https://mirror.kokuwa.io/maven2}" > "$HOME/.m2/settings.xml"
COPY . .
RUN --mount=type=cache,target=/tmp/mvn-repo mvn package -DskipTests -P=-dev

FROM busybox:1.38.0-uclibc@sha256:b699b0c739b2d039afd8e2488a3ccfc0b9a86301d87633448d4a93c57bee8c76
COPY --from=build --chmod=444 /build/target/keycloak-event-metrics.jar /opt/keycloak/providers/keycloak-event-metrics.jar
