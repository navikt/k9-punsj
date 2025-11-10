FROM ghcr.io/navikt/sif-baseimages/java-21:2025.11.10.1734Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY target/*.jar app.jar