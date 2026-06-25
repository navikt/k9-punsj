FROM ghcr.io/navikt/sif-baseimages/java-25:2026.06.24.1245Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY target/*.jar app.jar