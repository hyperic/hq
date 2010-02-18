#!/bin/sh
#
# Start/Stop the HQ container
#
doStart () {
  CATALINA_OPTS="${HQ_JAVA_OPTS} -Dhq.server.home=${SERVER_HOME} -Dcatalina.config=file://${ENGINE_HOME}/hq-server/conf/hq-catalina.properties" \
  CATALINA_PID="${SERVER_PID}" \
  ${ENGINE_HOME}/hq-server/bin/startup.sh >/dev/null
}

doStop() {
  #Shutdown through script rarely works.  Do a soft kill instead.
  if [ -f "${SERVER_PID}" ] ; then
    HQPID=`cat ${SERVER_PID} | tr -d ' '`
    if [ "x${HQPID}" = "x" ] ; then
      infoOut "HQ pid file was empty: ${SERVER_PID}"
      exit 127
    fi
    kill ${HQPID} 2> /dev/null
  else 
    infoOut "HQ server not running (no pid file found)"
  fi  
}

doHalt() {
  CATALINA_PID="${SERVER_PID}" \
  ${ENGINE_HOME}/hq-server/bin/shutdown.sh -force >/dev/null
}

infoOut () {
  echo "${@}"
}

case "$1" in
  start)
    doStart
	;;
	stop)
	doStop
	;;
	halt)
	doHalt
	;;
	*)
	exit 1
	;;
esac
exit 0
	