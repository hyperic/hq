#
# Script to start JBoss 3.x under optimizeIt
#

if [ "x" = "x$OPTIT_HOME" ]; then
   echo "OPTIT_HOME not set, exiting"
   exit 1
fi

if [ "x" = "x$JAVA_HOME" ]; then
   echo "JAVA_HOME not set, exiting"
fi

if [ "x" = "x$JBOSS_HOME" ]; then
   echo "JBOSS_HOME not set, exiting"
fi 

MAINCLASS=org.jboss.Main
AUDIT_MAIN_CLASS=intuitive.audit.Audit

# Setup the JVM
if [ "x$JAVA_HOME" != "x" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA="java"
fi

LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$OPTIT_HOME/lib
export LD_LIBRARY_PATH

JBOSS_CLASSPATH=$JBOSS_HOME/bin/run.jar
OPTIT_CLASSPATH=$OPTIT_HOME/lib/optit.jar

COMMAND="$JAVA -Xrunpri -Xbootclasspath/a:$OPTIT_HOME/lib/oibcp.jar"
JAVA_OPTS="$JAVA_OPTS -DGCOPSIZE=5"
JAVACP=$JAVA_HOME/lib/tools.jar:$JBOSS_CLASSPATH:$OPTIT_CLASSPATH

cd $JBOSS_HOME/bin && $COMMAND $JAVA_OPTS \
  $JAVA_ARGS -classpath $JAVACP $AUDIT_MAIN_CLASS $MAINCLASS

