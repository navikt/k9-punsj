FROM ghcr.io/navikt/sif-baseimages/java-25:2026.06.12.1040Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY target/*.jar app.jar