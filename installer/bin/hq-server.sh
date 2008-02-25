#!/bin/sh
#
# Start/Stop the HQ server.
#

debugOut () {
  if [ "x${DEBUG}" = "x" ] ; then return; fi
  echo "DEBUG: $@"
}
infoOut () {
  echo "${@}"
}

#
# Load JAVA_OPTS.  Changes to this file will be lost on server upgrades.  To set this
# value permanently, set server.java.opts in conf/hq-server.conf
#
loadJavaOpts () {
  TMPPROPFILE="${SERVER_HOME}/logs/.hq-server.conf.tmp"
  cat ${SERVER_HOME}/conf/hq-server.conf | grep server\.java\.opts | grep -v "^#" | sed 's/\./_/g' > ${TMPPROPFILE}
  . ${TMPPROPFILE} 2> /dev/null
  rm -f ${TMPPROPFILE}
  if [ "x${server_java_opts}" = "x" ] ; then
    echo "-XX:MaxPermSize=192m -Xmx512m -Xms512m"
  fi
  echo "${server_java_opts}"
}

loadWebappPort () {
  TMPPROPFILE="${SERVER_HOME}/logs/.hq-server.conf.tmp"
  cat ${SERVER_HOME}/conf/hq-server.conf | grep server\.webapp\.port | tr -d ' \t' | grep -v "^#" | sed 's/\./_/g' > ${TMPPROPFILE}
  . ${TMPPROPFILE} 2> /dev/null
  rm -f ${TMPPROPFILE}
  if [ "x${server_webapp_port}" = "x" ] ; then
    exit 127
  fi
  echo "${server_webapp_port}"
}

loadDBPort () {

  TMPPROPFILE="${SERVER_HOME}/hqdb/data/.postgresql.conf.tmp"
  DBCONF="${SERVER_HOME}/hqdb/data/postgresql.conf"
  cat ${DBCONF} | grep port | tr -d ' \t' | grep -v '^#' | sed 's/#.*//g' > ${TMPPROPFILE}
  . ${TMPPROPFILE}
  rm -f ${TMPPROPFILE}
  if [ "x${port}" = "x" ] ; then
    # they must have commented it out or removed the line, so assume it's on the default port.
    echo "5432"
  fi
  echo "${port}"
}

checkPort () {
  PORTNUM=${1}
  if [ "x${PORTNUM}" = "x" ] ; then
    infoOut "No port specified to checkPort function."
    exit 127
  fi

  THISOS=`uname -s`
  ISLISTENING=0
  case "x${THISOS}" in
    xLinux)
      ISLISTENING=`netstat -nlp 2> /dev/null | grep ":${PORTNUM}" | wc -l | tr -d ' '`
      ;;
    *)
      # Works on Solaris, HP-UX, Darwin and FreeBSD, possibly AIX
      ISLISTENING=`netstat -an 2> /dev/null | grep "\.${PORTNUM}" | head -n 1 | wc -l | tr -d ' '`
      ;;
  esac
  debugOut "checkPort: ISLISTENING=${ISLISTENING}"
  if [ ${ISLISTENING} -gt 0 ] ; then return 1; fi
  if [ ${ISLISTENING} -eq 0 ] ; then return 0; fi
  infoOut "Error checking for process listening on port ${PORTNUM}"
  exit 4
}

waitForPid () {
  HQPID=${1}
  MAXTRIES=${2}

  TRIES=0
  debugOut "waitForPid: waiting for ${HQPID}"
  while [ 1 -eq 1 ]; do
    PIDCHECK=`kill -0 ${HQPID} 2> /dev/null`
    if [ $? -eq 1 ]; then
      infoOut "HQ server PID ${HQPID} exited"
      return 1
    fi
 
    debugOut "waitForPid: PID ${HQPID} still alive" 
    sleep 2
    TRIES=`expr ${TRIES} + 1`
    if [ ${TRIES} -ge ${MAXTRIES} ] ; then
       debugOut "num TRIES exhausted: ${TRIES} -ge ${MAXTRIES}"
       break
    fi
  done

  infoOut "HQ server PID ${HQPID} did not exit."
  return 0;
}

waitForPort () {

  PORTNUM=${1}
  MAXTRIES=${2}
  if [ "x${PORTNUM}" = "x" ] ; then
    infoOut "No port specified to waitForPort function."
    exit 127
  fi
  if [ "x${MAXTRIES}" = "x" ] ; then
    MAXTRIES=10
  fi
  ERRMSG=${3}
  ERRFILE=${4}
  if [ "x${ERRFILE}" = "x" ] ; then
    ERRFILE=""
  fi
  EXPECTEDUP=${5}
  if [ "x${EXPECTEDUP}" = "x" ] ; then
    EXPECTEDUP=1
  fi
  ACTION="start"
  if [ ${EXPECTEDUP} -eq 0 ] ; then
    ACTION="stop"
  fi

  WASSTARTED=0
  TRIES=0
  debugOut "waitForPort ${PORTNUM}/${EXPECTEDUP}, entering wait loop: TRIES=${TRIES}, MAXTRIES=${MAXTRIES}"
  while [ 1 -eq 1 ] ; do
    debugOut "checking port: ${PORTNUM}/${EXPECTEDUP}..."
    checkPort ${PORTNUM} 
    WASSTARTED=$?
    debugOut "status of ${PORTNUM}/${EXPECTEDUP} == ${WASSTARTED}"
    
    if [ ${WASSTARTED} -eq ${EXPECTEDUP} ] ; then
      debugOut "port was as expected: wasStarted=${WASSTARTED} -eq expectedUp=${EXPECTEDUP}"
      return 1
    fi
    sleep 2
    TRIES=`expr ${TRIES} + 1`
    if [ ${TRIES} -ge ${MAXTRIES} ] ; then
       debugOut "num TRIES exhausted: ${TRIES} -ge ${MAXTRIES}"
       break
    fi
  done
  if [ ${WASSTARTED} -ne ${EXPECTEDUP} ] ; then
    if [ "x${ERRMSG}" = "x" ] ; then
      infoOut "Error: Process did not ${ACTION} listening on port ${PORTNUM}"
    else 
      infoOut "${ERRMSG}"
    fi
    if [ "x${ERRFILE}" = "x" ] ; then
      infoOut "" # "No further error information available."
    else
      ERRFILE_EX=`eval "echo ${ERRFILE}"`
      infoOut "The log file ${ERRFILE_EX} may contain further details on why it failed to ${ACTION}."
    fi
    return 0
  fi
}

startBuiltinDB () {
  DBPIDFILE="${SERVER_HOME}/hqdb/data/postmaster.pid"
  debugOut "Checking existence of pidfile: ${DBPIDFILE}"
  if [ -f "${DBPIDFILE}" ] ; then
    DBPID=`head -n 1 ${DBPIDFILE}`
    if [ ! "x${DBPID}" = "x" ]; then
      # First check for stale pid file
      DBPIDCHECK=`kill -0 ${DBPID} 2> /dev/null`
      if [ $? -eq 1 ]; then
        infoOut "Removing stale pid file ${DBPIDFILE}"
        rm -f ${DBPIDFILE}
      else
        infoOut "HQ built-in database already running (pid file found: ${DBPIDFILE}), not starting it again." 
      fi
    fi
  fi 

  if [ ! -f "${DBPIDFILE}" ] ; then
    infoOut "Starting HQ built-in database..."
    ${SERVER_HOME}/bin/db-start.sh

    debugOut "loading dbport..."
    DBPORT=`loadDBPort`
    debugOut "loaded dbport=${DBPORT}"
    waitForPort ${DBPORT} 10 'HQ built-in database failed to start:' '${SERVER_HOME}/hqdb/data/hqdb.log' 1
    if [ $? -eq 0 ] ; then
      exit 1
    fi
    infoOut "HQ built-in database started."
  fi
}

doStart () {
 
# Is the server already running?
debugOut "checking pidfile exists: ${SERVER_PID}"
if [ -f "${SERVER_PID}" ] ; then
  HQPID=`cat ${SERVER_PID} | tr -d ' '`
  if [ ! "x${HQPID}" = "x" ] ; then
    PIDCHECK=`kill -0 ${HQPID} 2> /dev/null`
    if [ $? -eq 1 ]; then
      infoOut "Removing stale pid file ${SERVER_PID}"
      rm -f ${SERVER_PID}
    else 
      infoOut "HQ server is already running (pid ${HQPID})."
      exit 0
    fi
  fi
fi

# Setup the config based on conf/hq-server.conf
infoOut "Initializing HQ server configuration..."
ANT_OPTS="$ANT_OPTS -Djava.net.preferIPv4Stack=true" ANT_ARGS="" JAVA_HOME=${JAVA_HOME} ${ANT_HOME}/bin/ant --noconfig -q \
  -Dserver.home=${SERVER_HOME} \
  -Dengine.home=${ENGINE_HOME} \
  -logger org.hyperic.tools.ant.installer.InstallerLogger \
  -f ${SERVER_HOME}/data/server.xml sh-setup

# Start the database if we have a hqdb dir, and if it's not running
if [ -d "${SERVER_HOME}/hqdb" ] ; then
  debugOut "Calling startBuiltinDB"
  startBuiltinDB
  debugOut "startBuiltinDB completed"
fi

# Setup HQ_JAVA_OPTS from hq-server.conf
HQ_JAVA_OPTS=`loadJavaOpts`

# Enable the 64-bit JRE on Solaris 64-bit OS
THISOS=`uname -s`

if [ $THISOS = "SunOS" ] ; then
	ARCH=`isainfo -kv`
	
	case $ARCH in
		*64-bit*)
		  echo "Setting -d64 JAVA OPTION to enable SunOS 64-bit JRE"
			HQ_JAVA_OPTS="${HQ_JAVA_OPTS} -d64"
			;;
	esac
fi

# Start the server
infoOut "Booting the HQ server (Using JAVA_OPTS=${HQ_JAVA_OPTS})..."
cd ${SERVER_HOME}/hq-engine/bin
JBOSSUS=RUNASIS \
JBOSSHOME=${ENGINE_HOME} \
  ${JAVA} -server \
    ${HQ_JAVA_OPTS} \
    -Dprogram.name=hq-server \
    -Dserver.home=${SERVER_HOME} \
    -Dengine.home=${ENGINE_HOME} \
    -Djava.awt.headless=true \
    -Djava.net.preferIPv4Stack=true \
    -Dinstantj.compile.compiler=instantj.compile.pizza.PizzaSourceCompiler \
    -classpath ${ENGINE_HOME}/bin/run.jar \
    org.jboss.Main > ${SERVER_LOG} 2>&1 &

  # Save the pid to a pidfile
  HQPID=$!
  echo "${HQPID}" > ${SERVER_PID}

  # Wait for the webapp to come up
  debugOut "Waiting for webapp port to come up..."
  WEBAPP_PORT=`loadWebappPort`
  debugOut "Loaded WEBAPP_PORT=${WEBAPP_PORT}"
  waitForPort ${WEBAPP_PORT}  90 'HQ failed to start' '${SERVER_LOG}' 1
  if [ $? -eq 0 ] ; then
    exit 1
  fi
}

doStopSignal () {

  SIGNAME=${1}
  if [ "x${SIGNAME}" = "x" ] ; then
    infoOut "No signal specified"
    exit 127
  fi

  # Do we have a pidfile?
  debugOut "checking pidfile exists: ${SERVER_PID}"
  if [ -f "${SERVER_PID}" ] ; then
    HQPID=`cat ${SERVER_PID} | tr -d ' '`
    if [ "x${HQPID}" = "x" ] ; then
      infoOut "HQ pid file was empty: ${SERVER_PID}"
      exit 127
    fi
    kill -${SIGNAME} ${HQPID} 2> /dev/null
    
    waitForPid ${HQPID} 60
    if [ $? -eq 0 ] ; then
      exit 1
    fi
    rm -f ${SERVER_PID}
  else 
    infoOut "HQ server not running (no pid file found: ${SERVER_PID})"
  fi

  # Stop builtin db if there is one
  debugOut "checking hqdb dir exists: ${SERVER_HOME}/hqdb"
  if [ -d ${SERVER_HOME}/hqdb ] ; then
    DBPIDFILE="${SERVER_HOME}/hqdb/data/postmaster.pid"
    debugOut "checking db pidfile exists: ${DBPIDFILE}"
    if [ -f ${DBPIDFILE} ] ; then
      debugOut "db pidfile exists ${DBPIDFILE}"
      infoOut "Stopping HQ built-in database..."
      ${SERVER_HOME}/bin/db-stop.sh
      DBPORT=`loadDBPort`
      waitForPort ${DBPORT} 30 'HQ built-in database failed to stop:' '${SERVER_HOME}/logs/hqdb.log' 0
      if [ $? -eq 0 ] ; then
        exit 1
      fi
    else
      infoOut "HQ built-in database not running (no pid file found: ${DBPIDFILE})"
    fi
  fi
}

doStop () {
  doStopSignal "TERM"
}

doHalt () {
  doStopSignal "KILL"
}

cd `dirname $0`/..
SERVER_HOME=`pwd`
ENGINE_HOME="${SERVER_HOME}/hq-engine"
SERVER_LOG="${SERVER_HOME}/logs/server.out"
SERVER_PID_DIR="${SERVER_HOME}/logs"
SERVER_PID="${SERVER_PID_DIR}/hq-server.pid"

if [ "x${HQ_JAVA_HOME}" != "x" ] ; then
    JAVA_HOME=${HQ_JAVA_HOME}
elif [ -d ${SERVER_HOME}/jre ]; then
    JAVA_HOME=${SERVER_HOME}/jre
elif [ "x$JAVA_HOME" = "x" ] ; then
    case "`uname`" in
    Darwin)
        JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
        ;;
    *)
        echo "JAVA_HOME or HQ_JAVA_HOME must be set when invoking the server"
        exit 1
        ;;
    esac
fi

JAVA="${JAVA_HOME}/bin/java"
if [ ! -x "${JAVA}" ] ; then
    echo "${JAVA} does not exist or is not executable."
    exit 1
fi

ANT_HOME=${SERVER_HOME}
export ANT_HOME
action=
if [ "x${2}" = "x-debug" ] ; then
  DEBUG=1
fi

case "$1" in
  start)
    echo "Starting HQ server..."
    doStart
    echo "HQ server booted."
    echo "Login to HQ at: http://127.0.0.1:${WEBAPP_PORT}/"
    ;;
  stop)
    echo "Stopping HQ server..."
    doStop
    echo "HQ server is stopped."
    ;;
  halt)
    echo "Halting HQ server..."
    doHalt
    echo "HQ server is halted."
    ;;
  *)
    # Print help, don't advertise halt, it's nasty
    echo "Usage: $0 {start|stop}" 1>&2
    exit 1
    ;;
esac

exit 0
