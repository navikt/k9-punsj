FROM ghcr.io/navikt/sif-baseimages/java-21:2025.11.04.1325Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY target/*.jar app.jar