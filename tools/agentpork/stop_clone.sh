#!/bin/sh

. etc/multiagent.properties

cd clones/clone_$1
./hq-agent-nowrapper.sh stop
