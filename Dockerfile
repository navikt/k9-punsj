FROM gcr.io/distroless/java17-debian11:nonroot
LABEL org.opencontainers.image.source=https://github.com/navikt/k9-punsj

WORKDIR /app
COPY target/*.jar app.jar

CMD [ "app.jar" ]