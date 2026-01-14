FROM ghcr.io/navikt/sif-baseimages/java-21:2026.01.14.1455Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY target/*.jar app.jar