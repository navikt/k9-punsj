#!/usr/bin/env sh

echo "Importing systembruker credentials"

if test -f /var/run/secrets/nais.io/srvk9punsj/password;
then
    export  SYSTEMBRUKER_PASSWORD=$(cat /var/run/secrets/nais.io/srvk9punsj/password)
    echo "Setting SYSTEMBRUKER_PASSWORD"
fi

if test -f /var/run/secrets/nais.io/srvk9punsj/username;
then
    export  SYSTEMBRUKER_USERNAME=$(cat /var/run/secrets/nais.io/srvk9punsj/username)
    echo "Setting SYSTEMBRUKER_USERNAME"
fi
