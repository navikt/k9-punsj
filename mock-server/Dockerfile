FROM ghcr.io/navikt/baseimages/temurin:17-appdynamics

COPY src/main/resources ./static
COPY target/k9-punsj-mock-server.jar ./app.jar
ENV K9_MOCKS_ROOT_DIR=static