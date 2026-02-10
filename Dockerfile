FROM ghcr.io/navikt/sif-baseimages/java-21:2026.02.06.0908Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY target/*.jar app.jar