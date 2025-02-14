FROM ghcr.io/navikt/sif-baseimages/java-21:2025.02.13.1522Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY docker-init-scripts/import-serviceuser-credentials.sh /init-scripts/21-import-serviceuser-credentials.sh

COPY target/*.jar app.jar