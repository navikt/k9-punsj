FROM ghcr.io/navikt/sif-baseimages/java-chainguard-25:2026.11.19.0715Z
LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

WORKDIR /app
COPY target/*.jar app.jar
