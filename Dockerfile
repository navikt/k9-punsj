FROM ghcr.io/navikt/baseimages/temurin:21

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY docker-init-scripts/import-serviceuser-credentials.sh /init-scripts/21-import-serviceuser-credentials.sh

COPY target/*.jar app.jar