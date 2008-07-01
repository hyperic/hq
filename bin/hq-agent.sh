#!/bin/sh

AGENTPROPFILE_PROP=agent.propFile
AGENT_PROPS=../../conf/agent.properties
AGENTLOGDIR_PROP=agent.logDir
AGENTLOGDIR=../../log
AGENTDATADIR_PROP=agent.dataDir
AGENTDATADIR=../../data
AGENT_LIB=./lib
PDK_LIB=./pdk/lib
WRAPPER_LIB=../../wrapper/lib
# for /proc/net/tcp mirror
SIGAR_PROC_NET=./tmp

# ------------- 
# Shouldn't need to change anything below this
# -------------

FINDNAME=$0 
while [ -h $FINDNAME ] ; do FINDNAME=`ls -ld $FINDNAME | awk '{print $NF}'` ; done 
RUNDIR=`echo $FINDNAME | sed -e 's@/[^/]*$@@'` 
unset FINDNAME
if test -d $RUNDIR; then
  cd $RUNDIR/..
else
  cd ..
fi

if [ "x${HQ_JAVA_HOME}" != "x" ] ; then
    HQ_JAVA_HOME=${HQ_JAVA_HOME}
elif [ -d jre ]; then
    HQ_JAVA_HOME=jre
    # Just in case
    chmod -R +x jre/bin/*
elif [ "x$JAVA_HOME" != "x" ] ; then
    HQ_JAVA_HOME=${JAVA_HOME}
else
    case "`uname`" in
    Darwin)
        HQ_JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
        ;;
    *)
        echo "HQ_JAVA_HOME or JAVA_HOME must be set when invoking the agent"
        exit 1
        ;;
    esac
fi

chmod +x ./pdk/scripts/*

HQ_JAVA="${HQ_JAVA_HOME}/bin/java"

CLIENT_CLASSPATH=
for i in `ls ${AGENT_LIB}/*.jar`
do
  CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${i}"
done

for i in `ls ${PDK_LIB}/*.jar`
do
  CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${i}"
done

for i in `ls ${WRAPPER_LIB}/*.jar`
do
  CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${i}"
done

CLIENT_CLASS=org.hyperic.hq.bizapp.agent.client.AgentClient

HQ_JAVA_OPTS="${HQ_JAVA_OPTS} \
    -Djava.net.preferIPv4Stack=true \
    -Dagent.mode=thread \
    -Dagent.install.home=../.. \
    -Dagent.bundle.home=. \
    -Djava.library.path=${WRAPPER_LIB}
    -D${AGENTPROPFILE_PROP}=${AGENT_PROPS} \
    -D${AGENTLOGDIR_PROP}=${AGENTLOGDIR} \
    -D${AGENTDATADIR_PROP}=${AGENTDATADIR}"

CLIENT_CMD="${HQ_JAVA} \
    ${HQ_JAVA_OPTS}
    -cp ${CLIENT_CLASSPATH} ${CLIENT_CLASS}"

START_CMD="${CLIENT_CMD} start"
STATUS_CMD="${CLIENT_CMD} status"
PING_CMD="${CLIENT_CMD} ping"
SETUP_CMD="${CLIENT_CMD} setup"
DIE_CMD="${CLIENT_CMD} die"

if [ "$1" = "start" ] ; then
    echo "Starting agent"
    if ${START_CMD} ; then
        exit 0        
    else
        exit 1
    fi
elif [ "$1" = "run" ] ; then
    echo "Running agent"
    echo ${START_CMD} 
    ${START_CMD} 
elif [ "$1" = "stop" ] ; then
    ${DIE_CMD} 10 || exit 1
elif [ "$1" = "status" ] ; then
    ${STATUS_CMD} || exit 1
elif [ "$1" = "ping" ] ; then
    echo Pinging ...
    VAL=`$PING_CMD 2>&1`
    if [ "$?" = "0" ] ; then
        echo 'Success!'
    else
        echo 'Failure!'
        echo $VAL
        exit 1
    fi
elif [ "$1" = "setup" ] ; then
    ${SETUP_CMD}
else
    echo "Syntax: $0 "'<start | stop | ping | setup>'
    exit 1
fi
