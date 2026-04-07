FROM maven:3.9.14-eclipse-temurin-17@sha256:31910a78e4fd727f631136abba113b3ec3d02843bb468357f379ec7313b348c1 AS build
SHELL ["/usr/bin/bash", "-e", "-u", "-c"]
WORKDIR /build
ARG MAVEN_ARGS="--batch-mode --color=always --no-transfer-progress"
ARG MAVEN_MIRROR_CENTRAL
RUN mkdir "$HOME/.m2" && printf "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\
<settings>\n\
	<localRepository>/tmp/mvn-repo</localRepository>\n\
	<mirrors><mirror><url>%s</url><mirrorOf>central</mirrorOf></mirror></mirrors>\n\
</settings>" "${MAVEN_MIRROR_CENTRAL:-https://mirror.kokuwa.io/maven2}" > "$HOME/.m2/settings.xml"
COPY . .
RUN --mount=type=cache,target=/tmp/mvn-repo mvn package -DskipTests -P=-dev

FROM busybox:1.37.0-uclibc@sha256:e3a499df044f0129fd2e82a63177e7affe66ba6fd73acc9e231fd7695f06e5e2
COPY --from=build --chmod=444 /build/target/keycloak-event-metrics.jar /opt/keycloak/providers/keycloak-event-metrics.jar
