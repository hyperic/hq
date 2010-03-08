#!/bin/bash

. etc/multiagent.properties

for ((i=0;i<$NUM_CLONES;i++))
do
	cd clones/clone_$i
	nohup ./hq-agent-nowrapper.sh start 2>&1 > console.out &
	echo $! > nowrapper.pid
	sleep 2
	cd ../..
done
