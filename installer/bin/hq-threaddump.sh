#!/bin/sh
#
# Saves HQ threaddump to $SERVER_HOME/logs/server.out
#

cd `dirname $0`/..
SERVER_HOME=`pwd`

if [ -f ${SERVER_HOME}/logs/hq-server.pid ]; then
    SERVER_PID=`cat ${SERVER_HOME}/logs/hq-server.pid`

    echo "Running kill -QUIT on ${SERVER_PID}"
    kill -QUIT ${SERVER_PID}
else
    echo "Server not running"
fi
