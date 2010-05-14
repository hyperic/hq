#!/bin/sh

. etc/multiagent.properties

echo "starting clone $1"

cd clones/clone_$1
nohup ./hq-agent-nowrapper.sh start 2>&1 > console.out & 
