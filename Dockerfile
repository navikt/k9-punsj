FROM ghcr.io/navikt/sif-baseimages/java-21:2025.04.28.1516Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY docker-init-scripts/import-serviceuser-credentials.sh /init-scripts/21-import-serviceuser-credentials.sh

COPY target/*.jar app.jar