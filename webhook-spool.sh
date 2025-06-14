#!/bin/sh
java -jar /webhook-router.jar --config /webhook-router.cfg || sleep infinity
echo -e "\nStart webhook monitor"

SPOOL=/var/spool/webhook/
java -jar /webhook-router.jar --config /webhook-router.cfg --spool $SPOOL

while read FILE; do
 java -jar /webhook-router.jar --config /webhook-router.cfg --spool $SPOOL
done < <(inotifywait -qme MOVED_TO --format '%f' /var/spool/webhook/)
