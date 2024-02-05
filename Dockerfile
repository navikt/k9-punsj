FROM ghcr.io/navikt/baseimages/temurin:21-appdynamics@sha256:5805ced01cf05bc2e5c2c35a8cdb21c8ce7ce0eb50557006fe12c3e03acfbbb0

ENV APPD_ENABLED=true
LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY docker-init-scripts/import-serviceuser-credentials.sh /init-scripts/21-import-serviceuser-credentials.sh
COPY docker-init-scripts/import-appdynamics-settings.sh /init-scripts/22-import-appdynamics-settings.sh


COPY target/*.jar app.jar
