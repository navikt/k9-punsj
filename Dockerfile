FROM ghcr.io/navikt/sif-baseimages/java-21:2025.11.18.1421Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY target/*.jar app.jar