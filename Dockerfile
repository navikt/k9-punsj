FROM ghcr.io/navikt/baseimages/temurin:21-appdynamics

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

RUN curl -L -O https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.6.0/opentelemetry-javaagent.jar

COPY docker-init-scripts/import-serviceuser-credentials.sh /init-scripts/21-import-serviceuser-credentials.sh
COPY docker-init-scripts/import-appdynamics-settings.sh /init-scripts/22-import-appdynamics-settings.sh
COPY docker-init-scripts/setup-opentelemetry.sh /init-scripts/23-setup-opentelemetry.sh

COPY target/*.jar app.jar