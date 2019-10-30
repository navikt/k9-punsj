#!/usr/bin/env sh

echo "Importing Azure credentials"

if test -d /var/run/secrets/nais.io/azuread;
then
    for FILE in /var/run/secrets/nais.io/azuread/*
    do
        _oldIFS=$IFS
        IFS='
'
        FILE_NAME=$(echo $FILE | sed 's:.*/::')
        KEY=AZURE_$FILE_NAME
        VALUE=$(cat "$FILE")

        echo "- exporting $KEY"
        export "$KEY"="$VALUE"

        IFS=$_oldIFS
    done
fi