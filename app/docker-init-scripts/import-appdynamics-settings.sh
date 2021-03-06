#!/usr/bin/env sh

echo "Importing Appdynamics settings"

if test -d /var/run/secrets/nais.io/appdynamics ;
then
    for FILE in $(find /var/run/secrets/nais.io/appdynamics -maxdepth 1 -name "*.env")
    do
        _oldIFS=$IFS
        IFS='
'
        for line in $(cat "$FILE"); do
            _key=${line%%=*}
            _val=${line#*=}

            if test "$_key" != "$line"
            then
                echo "- exporting $_key"
            else
                echo "- (warn) exporting contents of $FILE which is not formatted as KEY=VALUE"
            fi

            export "$_key"="$(echo "$_val"|sed -e "s/^['\"]//" -e "s/['\"]$//")"
        done
        IFS=$_oldIFS
    done
fi
