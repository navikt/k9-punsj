FROM ghcr.io/navikt/sif-baseimages/java-21:2026.01.29.1157Z

LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY target/*.jar app.jar