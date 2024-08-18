#!/usr/bin/env bash

#start opentelemetry-agenten dersom konfigurert
set +u
if [ ! -z "${OTEL_EXPORTER_OTLP_ENDPOINT}" ]; then
    echo "Configuring attatchment of opentelemetry-agent"
    JAVA_OPTS="${JAVA_OPTS} -javaagent:/app/opentelemetry-javaagent.jar"
fi
