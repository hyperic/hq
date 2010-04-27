#!/bin/sh

#for use by startup scripts such as jboss which do not background themselves.
#see ServerControlPlugin

exec "$@" &

PID=$!

if [ "x$HQ_CONTROL_WAIT" = "x" ] ; then
    exit 0
fi
if [ "x$HQ_CONTROL_WAIT" = "x0" ] ; then
    exit 0
fi

#sleep for a bit then check that the process is still alive,
#otherwise the script failed right away perhaps due to syntax
#error, invalid command line option, missing JAVA_HOME, etc.

sleep $HQ_CONTROL_WAIT

if kill -0 $PID 2>/dev/null ; then
    exit 0
else
    exit 1
fi
