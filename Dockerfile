FROM ghcr.io/navikt/sif-baseimages/java-21:2025.06.04.0911Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY target/*.jar app.jar