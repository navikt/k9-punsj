FROM navikt/java:16-appdynamics

ENV APPD_ENABLED=true
LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

COPY docker-init-scripts/import-azure-credentials.sh /init-scripts/20-import-azure-credentials.sh
COPY docker-init-scripts/import-serviceuser-credentials.sh /init-scripts/21-import-serviceuser-credentials.sh
COPY docker-init-scripts/import-appdynamics-settings.sh /init-scripts/22-import-appdynamics-settings.sh


COPY target/app.jar ./
