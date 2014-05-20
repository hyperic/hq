#!/bin/sh
#
# Setup HQ on a system.
#

if [ "$USER" = "root" -o "$UID" = "0" -o "$EUID" = "0" ]; then 
    echo ""
    echo "Execution of HQ setup not allowed as the root user."
    echo "Please log in as a different user and re-run $0"
    echo ""
    exit 1;
fi

cd `dirname $0`/..
INSTALL_DIR=`pwd`

if [ "x${TMPDIR}" = "x" ]; then
    JRE_TMP=/tmp
else
    JRE_TMP=${TMPDIR}
fi

VAL=`touch ${JRE_TMP}/_hq > /dev/null 2>&1`
if [ "$?" = "0" ]; then
    rm ${JRE_TMP}/_hq;
else
    echo "Temporary directory $JRE_TMP is not writable."
    echo ""
    echo "Please set your TMPDIR environment variable to a directory that"
    echo "is writable by the current user."
    exit 1
fi

#locate the correct jre package for this platform.
platform=`uname -s`
machine=`uname -m`
case "$platform" in
    Linux )
        if [ "${machine}" = "x86_64" ]; then
            JRE_NAME=amd64-linux-1.7_51.tar.gz
        fi
        ;;
    SunOS )
        JRE_NAME=sparc-sun-solaris-1.7_51.tar.gz
        ;;
esac

HQ_JAVA_HOME=
USE_BUILTIN_JRE=0
# unpack jre to temp directory
if [ -f "${INSTALL_DIR}/jres/${JRE_NAME}" ] ; then
  USE_BUILTIN_JRE=1
  echo "Unpacking JRE to temporary directory ${JRE_TMP}/jre"
  CWD=`pwd`
  cd ${JRE_TMP}
  gunzip -c ${INSTALL_DIR}/jres/${JRE_NAME} | tar -xf -
  chmod +x ${JRE_TMP}/jre/bin/java
  cd ${CWD}
  HQ_JAVA_HOME=${JRE_TMP}/jre
else
  # No JRE, make sure we have a JAVA_HOME
  if [ "x${JAVA_HOME}" = "x" ] ; then
    case "`uname`" in
    Darwin)
      JAVA_HOME=/System/Library/Frameworks/JavaVM.framework/Home
      ;;
    *)
      echo "No JAVA_HOME environment variable is defined."
      exit 1
      ;;
    esac
  fi
  # Make sure the java binary is OK
  if [ ! -x "${JAVA_HOME}/bin/java" ] ; then
    echo "JAVA_HOME/bin/java does not exist or is not executable."
    exit 1
  fi
  HQ_JAVA_HOME=${JAVA_HOME}
fi

ANT_HOME=${INSTALL_DIR}
export ANT_HOME

INSTALL_MODE=quick
SETUP_FILE=
while [ ! "x${1}" = "x" ] ; do
  if [ "x${1}" = "x-full" ] ; then
    INSTALL_MODE="full"
  elif [ "x${1}" = "x-upgrade" ] ; then
    INSTALL_MODE=upgrade
  elif [ "x${1}" = "x-updateScale" ] ; then
    INSTALL_MODE=updateScale
    echo "Please enter the installation profile:
           1: small (less than 50 platforms)
           2: medium (50-250 platforms)
           3: large (larger than 250 platforms)"
    read PROFILE
    case $PROFILE in
	1)
   		PROFILE="small"
   		;;
	2)
   		PROFILE="medium"
   		;;
	3)	
		PROFILE="large"
   		;;
     esac
    echo "Please enter the current server installation directory"
    read SERVER_DIR
  elif [ "x${1}" = "x-postgresql" ] ; then
    INSTALL_MODE=postgresql
  else
    SETUP_FILE="${1}"
  fi
  shift
done

echo "Please ignore references to missing tools.jar"
if [ "x${INSTALL_MODE}" = "xupdateScale" ] ; then
  ANT_OPTS="$ANT_OPTS -Djava.net.preferIPv4Stack=true" ANT_ARGS="" JAVA_HOME=${HQ_JAVA_HOME} ${ANT_HOME}/bin/ant --noconfig -q \
    -Dinstall.dir=${INSTALL_DIR} \
    -Dinstall.mode=${INSTALL_MODE} \
    -Dinstall.profile=${PROFILE} \
    -Dserver.product.dir=${SERVER_DIR} \
    -logger org.hyperic.tools.ant.installer.InstallerLogger \
    -f ${INSTALL_DIR}/data/setup.xml update-hq-server-profile
elif [ "x${SETUP_FILE}" = "x" ] ; then
  ANT_OPTS="$ANT_OPTS -Djava.net.preferIPv4Stack=true" ANT_ARGS="" JAVA_HOME=${HQ_JAVA_HOME} ${ANT_HOME}/bin/ant --noconfig -q \
    -Dinstall.dir=${INSTALL_DIR} \
    -Dinstall.mode=${INSTALL_MODE} \
    -logger org.hyperic.tools.ant.installer.InstallerLogger \
    -f ${INSTALL_DIR}/data/setup.xml
else 
  ANT_OPTS="$ANT_OPTS -Djava.net.preferIPv4Stack=true" ANT_ARGS="" JAVA_HOME=${HQ_JAVA_HOME} ${ANT_HOME}/bin/ant --noconfig -q \
    -Dinstall.dir=${INSTALL_DIR} \
    -Dsetup=${SETUP_FILE} \
    -Dinstall.mode=${INSTALL_MODE} \
    -logger org.hyperic.tools.ant.installer.InstallerLogger \
    -f ${INSTALL_DIR}/data/setup.xml
fi

if [ "${USE_BUILTIN_JRE}" = "1" ] ; then
  echo "Deleting temporary JRE"
  rm -Rf ${JRE_TMP}/jre
fi

exit 0
