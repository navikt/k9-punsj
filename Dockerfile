FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.26.0840Z
LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

WORKDIR /app
COPY target/*.jar app.jar
