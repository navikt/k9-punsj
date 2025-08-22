FROM ghcr.io/navikt/sif-baseimages/java-21:2025.08.07.1124Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY target/*.jar app.jar