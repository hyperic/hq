#!/bin/sh

AGENTPROPFILE_PROP=agent.propFile
AGENT_PROPS=../../conf/agent.properties
AGENTLOGDIR_PROP=agent.logDir
AGENTLOGDIR=../../log
AGENT_LIB=./lib
PDK_LIB=./pdk/lib
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

JDK13_LIBS="${AGENT_LIB}/jdk1.3-compat"
JDK13_COMPAT="${JDK13_LIBS}/jce1_2_2.jar"
JDK13_COMPAT="${JDK13_COMPAT}:${JDK13_LIBS}/sunjce_provider.jar"
JDK13_COMPAT="${JDK13_COMPAT}:${JDK13_LIBS}/jsse.jar"

CLIENT_CLASSPATH="${AGENT_LIB}/AgentClient.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/commons-logging.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/log4j-1.2.14.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/hyperic-util.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/sigar.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/hq-product.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/commons-httpclient-3.1.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${PDK_LIB}/commons-codec-1.3.jar"
CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${AGENT_LIB}/lather.jar"

CLIENT_CLASSPATH="${CLIENT_CLASSPATH}:${JDK13_COMPAT}"

CLIENT_CLASS=org.hyperic.hq.bizapp.agent.client.AgentClient

CLIENT_CMD="${HQ_JAVA} \
    -Djava.net.preferIPv4Stack=true \
    -D${AGENTPROPFILE_PROP}=${AGENT_PROPS} \
    -D${AGENTLOGDIR_PROP}=${AGENTLOGDIR} \
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
