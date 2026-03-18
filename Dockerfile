FROM ghcr.io/navikt/sif-baseimages/java-25:2026.03.16.1330Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY target/*.jar app.jar