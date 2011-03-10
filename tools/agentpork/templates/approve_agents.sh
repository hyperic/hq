#!/bin/bash

. etc/multiagent.properties

for ((i=0;i<$NUM_CLONES;i++))
do
    ./hqapi/bin/hqapi.sh autodiscovery approve --user $HQ_USER --password $HQ_PASSWORD --host $CLONE_SERVERIP --port $CLONE_SERVER_PORT --regex="$HOSTNAME.*grp$1-$i"
    sleep 20
done
