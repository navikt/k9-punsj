FROM navikt/java:13

COPY docker-init-scripts/import-azure-credentials.sh /init-scripts/20-import-azure-credentials.sh

COPY target/app.jar ./