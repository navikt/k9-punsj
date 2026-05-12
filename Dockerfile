FROM ghcr.io/navikt/sif-baseimages/java-25:2026.05.11.0700Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY target/*.jar app.jar