#!/bin/sh

THISDIR=`dirname $0`
cd ${THISDIR}
THISDIR=`pwd`
cd ..
CAM_HOME=`pwd`
cd ${THISDIR}

cygwin=false;
sep=":"
case "`uname`" in
    CYGWIN*)
      sep=";"
esac

# Make sure we have JAVA_HOME
if [ "${JAVA_HOME}" = "" ] ; then
  echo "Environment variable JAVA_HOME not defined."
  exit 1
fi

JDBC_LIB=${CAM_HOME}/thirdparty/lib
ORACLE_LIB="${JDBC_LIB}/oracle_jdbc"
SYBASE_LIB="${JDBC_LIB}/sybase_jdbc"
MYSQL_LIB="${JDBC_LIB}/mysql_jdbc"
POSTGRES_LIB="${JDBC_LIB}/postgresql"
JDBC_CLASSPATH="${JDBC_LIB}/henplus.jar"
JDBC_CLASSPATH="${JDBC_CLASSPATH}${sep}${JDBC_LIB}/libreadline-java.jar"

for f in $ORACLE_LIB/*.jar $SYBASE_LIB/*.jar $MYSQL_LIB/*.jar $POSTGRES_LIB/*.jar; do
    if [ -r "${f}" ] ; then
        JDBC_CLASSPATH="${JDBC_CLASSPATH}${sep}${f}"
    fi
done
JAVA="${JAVA_HOME}/bin/java"
JDBC_CLIENT_CLASS="henplus.HenPlus"

${JAVA} -classpath ${JDBC_CLASSPATH} ${JDBC_CLIENT_CLASS} $@

