#!/bin/sh

. etc/multiagent.properties

cd clones/clone_$1
# Stop the agent
nohup ./hq-agent-nowrapper.sh stop 2>&1 > console_stop.out &

# Cleanup in case agent nohup process is still running
# kill child process forked by hq-agent-nowrapper.sh
# This matches the java process and send it sighup, it also kills hq-agent-nowrapiper.sh
ps -o pid= --ppid `cat nowrapper.pid`|  xargs kill
rm nowrapper.pid
