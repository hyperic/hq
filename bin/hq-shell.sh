#!/bin/sh

CLIENT_HOME=.

RUNDIR=`echo $0 | sed -e 's@/[^/]*$@@'`
cd $RUNDIR

if [ "x${HQ_JAVA_HOME}" != "x" ] ; then
    JAVA_HOME=${HQ_JAVA_HOME}
elif [ -d jre ]; then
    JAVA_HOME=jre
elif [ "x$JAVA_HOME" = "x" ] ; then
    case "`uname`" in
    Darwin)
        JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
        ;;
    *)
        echo "HQ_JAVA_HOME or JAVA_HOME must be set when invoking the shell"
        exit 1
        ;;
    esac
fi

JAVA="${JAVA_HOME}/bin/java"

# Set LD_LIBRARY_PATH to enhance chances that we pickup libreadline
if [ "${LD_LIBRARY_PATH}" != "" ]; then
  LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/lib:/usr/lib:/usr/local/lib
else
  LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib
fi
export LD_LIBRARY_PATH

# Load all jars in lib
CLIENT_CP=
for jar in ${CLIENT_HOME}/lib/*.jar ; do
    if [ "$CLIENT_CP" != "" ]; then
        CLIENT_CP=${CLIENT_CP}:$jar
    else
        CLIENT_CP=$jar
    fi
done

# logging stuff NOTE: NoOpLog turns off logging, SimpleLog turns on logging.
LOG=org.apache.commons.logging.impl.NoOpLog
#LOG=org.apache.commons.logging.impl.SimpleLog
SYS_PROPS="-Dorg.apache.commons.logging.Log=${LOG} -Dlog4j.rootCategory=ERROR"

SYS_PROPS="${SYS_PROPS} -Dpage.size=$LINES -Djava.net.preferIPv4Stack=true"

MAIN_CLASS=org.hyperic.hq.bizapp.client.shell.ClientShell

${JAVA} ${SYS_PROPS} -classpath ${CLIENT_CP} ${MAIN_CLASS}
