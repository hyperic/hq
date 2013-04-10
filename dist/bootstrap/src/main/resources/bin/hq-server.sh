#! /bin/sh

#
# Copyright (c) 1999, 2006 Tanuki Software Inc.
#
# Java Service Wrapper sh script.  Suitable for starting and stopping
#  wrapped Java applications on UNIX platforms.
#

#-----------------------------------------------------------------------------
# These settings can be modified to fit the needs of your application

if [ "$USER" = "root" -o "$UID" = "0" -o "$EUID" = "0" ]; then 
    echo ""
    echo "Execution of HQ server not allowed as the root user."
    echo "Please log in as a different user and re-run $0 start"
    echo ""
    exit 1;
fi

# Application
APP_NAME="hq-server"
APP_LONG_NAME="HQ Server"

if [ `uname -s` = "SunOS" ]
then
  ECHO_NOCR="echo"
else
  ECHO_NOCR="echo -e"
fi

# Wrapper
WRAPPER_CMD="../../wrapper/sbin/wrapper"
WRAPPER_CMD_PS="sbin/wrapper"
WRAPPER_CONF="../../conf/wrapper.conf"

# Priority at which to run the wrapper.  See "man nice" for valid priorities.
#  nice is only used if a priority is specified.
PRIORITY=

# Location of the pid file.
PIDDIR="../../wrapper"

# If uncommented, causes the Wrapper to be shutdown using an anchor file.
#  When launched with the 'start' command, it will also ignore all INT and
#  TERM signals.
#IGNORE_SIGNALS=true

# Wrapper will start the JVM asynchronously. Your application may have some
#  initialization tasks and it may be desirable to wait a few seconds
#  before returning.  For example, to delay the invocation of following
#  startup scripts.  Setting WAIT_AFTER_STARTUP to a positive number will
#  cause the start command to delay for the indicated period of time 
#  (in seconds).
# 
WAIT_AFTER_STARTUP=0

# If set, the status, start_msg and stop_msg commands will print out detailed
#   state information on the Wrapper and Java processes.
#DETAIL_STATUS=true

# If specified, the Wrapper will be run as the specified user.
# IMPORTANT - Make sure that the user has the required privileges to write
#  the PID file and wrapper.log files.  Failure to be able to write the log
#  file will cause the Wrapper to exit without any way to write out an error
#  message.
# NOTE - This will set the user which is used to run the Wrapper as well as
#  the JVM and is not useful in situations where a privileged resource or
#  port needs to be allocated prior to the user being changed.
#RUN_AS_USER=

# The following two lines are used by the chkconfig command. Change as is
#  appropriate for your application.  They should remain commented.
# chkconfig: 2345 20 80
# description: @app.long.name@
 
# Initialization block for the install_initd and remove_initd scripts used by
#  SUSE linux distributions.
### BEGIN INIT INFO
# Provides: @app.name@
# Required-Start: $local_fs $network $syslog
# Should-Start: 
# Required-Stop:
# Default-Start: 2 3 4 5
# Default-Stop: 0 1 6
# Short-Description: @app.long.name@
# Description: @app.description@
### END INIT INFO

# Do not modify anything beyond this point
#-----------------------------------------------------------------------------

# Get the fully qualified path to the script
case $0 in
    /*)
        SCRIPT="$0"
        ;;
    *)
        PWD=`pwd`
        SCRIPT="$PWD/$0"
        ;;
esac

# Resolve the true real path without any sym links.
CHANGED=true
while [ "X$CHANGED" != "X" ]
do
    # Change spaces to ":" so the tokens can be parsed.
    SAFESCRIPT=`echo $SCRIPT | sed -e 's; ;:;g'`
    # Get the real path to this script, resolving any symbolic links
    TOKENS=`echo $SAFESCRIPT | sed -e 's;/; ;g'`
    REALPATH=
    for C in $TOKENS; do
        # Change any ":" in the token back to a space.
        C=`echo $C | sed -e 's;:; ;g'`
        REALPATH="$REALPATH/$C"
        # If REALPATH is a sym link, resolve it.  Loop for nested links.
        while [ -h "$REALPATH" ] ; do
            LS="`ls -ld "$REALPATH"`"
            LINK="`expr "$LS" : '.*-> \(.*\)$'`"
            if expr "$LINK" : '/.*' > /dev/null; then
                # LINK is absolute.
                REALPATH="$LINK"
            else
                # LINK is relative.
                REALPATH="`dirname "$REALPATH"`""/$LINK"
            fi
        done
    done

    if [ "$REALPATH" = "$SCRIPT" ]
    then
        CHANGED=""
    else
        SCRIPT="$REALPATH"
    fi
done

# resolve the current HQ Server home
cd "`dirname "$REALPATH"`/.."

SERVER_INSTALL_HOME=`pwd`
# invoke the Java Service Wrapper from the wrapper sbin
# directory for compatibility with Windows
cd wrapper/sbin
REALDIR=`pwd`

# ------------- 
# Begin HQ Server specific logic
# ------------- 

if [ "x${HQ_JAVA_HOME}" != "x" ] ; then
    JAVA_HOME=${HQ_JAVA_HOME}
elif [ -d ${SERVER_INSTALL_HOME}/jre ]; then
    JAVA_HOME=${SERVER_INSTALL_HOME}/jre
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

HQ_JAVA="${JAVA_HOME}/bin/java"
# verify that the java command actually exists
if [ ! -f "$HQ_JAVA" ]
then
        echo Invalid Java Home detected at ${JAVA_HOME}
        exit 1
fi

REQUIRED_VERSION_STRING=1.6
# Transform the required version string into a number that can be used in comparisons
REQUIRED_VERSION=`echo ${REQUIRED_VERSION_STRING} | sed -e 's;\.;0;g'`
# Check HQ_JAVA to see if Java version is adequate
if [ ${HQ_JAVA} ]
then
	${HQ_JAVA} -version 2> /tmp/tmp.ver
	VERSION=`cat /tmp/tmp.ver | grep "java version" | awk '{ print substr($3, 2, length($3)-2); }'`
	rm /tmp/tmp.ver
	VERSION=`echo $VERSION | awk '{ print substr($1, 1, 3); }' | sed -e 's;\.;0;g'`
	if [ ${VERSION} ]
	then
		if [ ${VERSION} -lt ${REQUIRED_VERSION} ]
		then
			echo "Java version must be at least ${REQUIRED_VERSION_STRING}."
			exit 1
		fi
	else
		echo "Java version was not found."
		exit 1
	fi
fi


# ------------- 
# End HQ specific logic
# ------------- 

# If the PIDDIR is relative, set its value relative to the full REALPATH to avoid problems if
#  the working directory is later changed.
FIRST_CHAR=`echo $PIDDIR | cut -c1,1`
if [ "$FIRST_CHAR" != "/" ]
then
    PIDDIR=$REALDIR/$PIDDIR
fi
# Same test for WRAPPER_CMD
FIRST_CHAR=`echo $WRAPPER_CMD | cut -c1,1`
if [ "$FIRST_CHAR" != "/" ]
then
    WRAPPER_CMD=$REALDIR/$WRAPPER_CMD
fi
# Same test for WRAPPER_CONF
FIRST_CHAR=`echo $WRAPPER_CONF | cut -c1,1`
if [ "$FIRST_CHAR" != "/" ]
then
    WRAPPER_CONF=$REALDIR/$WRAPPER_CONF
fi

# Process ID
ANCHORFILE="$PIDDIR/$APP_NAME.anchor"
STATUSFILE="$PIDDIR/$APP_NAME.status"
JAVASTATUSFILE="$PIDDIR/$APP_NAME.java.status"
PIDFILE="$PIDDIR/$APP_NAME.pid"
LOCKDIR="/var/lock/subsys"
LOCKFILE="$LOCKDIR/$APP_NAME"
pid=""

# Resolve the location of the 'ps' command
PSEXE="/usr/bin/ps"
if [ ! -x "$PSEXE" ]
then
    PSEXE="/bin/ps"
    if [ ! -x "$PSEXE" ]
    then
        echo "Unable to locate 'ps'."
        echo "Please report this message along with the location of the command on your system."
        exit 1
    fi
fi

# Resolve the os
DIST_OS=`uname -s | tr [:upper:] [:lower:] | tr -d [:blank:]`
DIST_BITS="32"
case "$DIST_OS" in
    'sunos')
        DIST_OS="solaris"
        ;;
    'hp-ux')
        # HP-UX needs the XPG4 version of ps (for -o args)
        DIST_OS="hpux"
        UNIX95=""
        export UNIX95
        ;;
    'hp-ux64')
        # HP-UX needs the XPG4 version of ps (for -o args)
        DIST_OS="hpux"
        UNIX95=""
        export UNIX95
        DIST_BITS="64"
        ;;
    'darwin')
        DIST_OS="macosx"
        ;;
    'unix_sv')
        DIST_OS="unixware"
        ;;
esac

# Resolve the architecture
if [ "$DIST_OS" = "macosx" ]
then
    DIST_ARCH="universal"
else
    DIST_ARCH=
    DIST_ARCH=`uname -p 2>/dev/null | tr [:upper:] [:lower:] | tr -d [:blank:]`
    if [ "X$DIST_ARCH" = "X" ]
    then
        DIST_ARCH="unknown"
    fi
    if [ "$DIST_ARCH" = "unknown" ]
    then
        DIST_ARCH=`uname -m 2>/dev/null | tr [:upper:] [:lower:] | tr -d [:blank:]`
    fi

    case "$DIST_ARCH" in
        'athlon' | 'i386' | 'i486' | 'i586' | 'i686')
            DIST_ARCH="x86"
            ;;
        'amd64' | 'x86_64')
            DIST_ARCH="x86"
            DIST_BITS="64"
            ;;            
        'ia32')
            DIST_ARCH="ia"
            ;;
        'ia64' | 'ia64n' | 'ia64w')
            DIST_ARCH="ia"
            DIST_BITS="64"
            ;;            
        'ip27')
            DIST_ARCH="mips"
            ;;
        'power' | 'powerpc' | 'power_pc' | 'ppc64')
            DIST_ARCH="ppc"
            ;;
        'pa_risc' | 'pa-risc')
            DIST_ARCH="parisc"
            ;;
        'sun4u' | 'sparcv9')
            DIST_ARCH="sparc"
            ;;
        '9000/800')
            DIST_ARCH="parisc"
            ;;
    esac
fi

outputFile() {
    if [ -f "$1" ]
    then
        echo "  $1 (Found but not executable.)";
    else
        echo "  $1"
    fi
}

# Decide on the wrapper binary to use.
# First, try out the detected bit version
# If not available, try 32 bits followed by 64 bits.
if [ -x "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-$DIST_BITS" ]
then
  WRAPPER_CMD="$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-$DIST_BITS"
elif [ -x "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-32" ]
then
  WRAPPER_CMD="$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-32"
elif [ -x "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-64" ]
then
  WRAPPER_CMD="$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-64"
else
  if [ ! -x "$WRAPPER_CMD" ]
    then
      echo "Unable to locate any of the following binaries:"
      outputFile "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-32"
      outputFile "$WRAPPER_CMD-$DIST_OS-$DIST_ARCH-64"
      outputFile "$WRAPPER_CMD"
      exit 1
  fi
fi

# Build the nice clause
if [ "X$PRIORITY" = "X" ]
then
    CMDNICE=""
else
    CMDNICE="nice -$PRIORITY"
fi

# Build the anchor file clause.
if [ "X$IGNORE_SIGNALS" = "X" ]
then
   ANCHORPROP=
   IGNOREPROP=
else
   ANCHORPROP=wrapper.anchorfile=\"$ANCHORFILE\"
   IGNOREPROP=wrapper.ignore_signals=TRUE
fi

# Build the status file clause.
if [ "X$DETAIL_STATUS" = "X" ]
then
   STATUSPROP=
else
   STATUSPROP="wrapper.statusfile=\"$STATUSFILE\" wrapper.java.statusfile=\"$JAVASTATUSFILE\""
fi

# Build the lock file clause.  Only create a lock file if the lock directory exists on this platform.
LOCKPROP=
if [ -d $LOCKDIR ]
then
    if [ -w $LOCKDIR ]
    then
        LOCKPROP=wrapper.lockfile=\"$LOCKFILE\"
    fi
fi

checkUser() {
    # $1 touchLock flag
    # $2 command

    # Check the configured user.  If necessary rerun this script as the desired user.
    if [ "X$RUN_AS_USER" != "X" ]
    then
        # Resolve the location of the 'id' command
        IDEXE="/usr/xpg4/bin/id"
        if [ ! -x "$IDEXE" ]
        then
            IDEXE="/usr/bin/id"
            if [ ! -x "$IDEXE" ]
            then
                echo "Unable to locate 'id'."
                echo "Please report this message along with the location of the command on your system."
                exit 1
            fi
        fi
    
        if [ "`$IDEXE -u -n`" = "$RUN_AS_USER" ]
        then
            # Already running as the configured user.  Avoid password prompts by not calling su.
            RUN_AS_USER=""
        fi
    fi
    if [ "X$RUN_AS_USER" != "X" ]
    then
        # If LOCKPROP and $RUN_AS_USER are defined then the new user will most likely not be
        # able to create the lock file.  The Wrapper will be able to update this file once it
        # is created but will not be able to delete it on shutdown.  If $2 is defined then
        # the lock file should be created for the current command
        if [ "X$LOCKPROP" != "X" ]
        then
            if [ "X$1" != "X" ]
            then
                # Resolve the primary group 
                RUN_AS_GROUP=`groups $RUN_AS_USER | awk '{print $3}' | tail -1`
                if [ "X$RUN_AS_GROUP" = "X" ]
                then
                    RUN_AS_GROUP=$RUN_AS_USER
                fi
                touch $LOCKFILE
                chown $RUN_AS_USER:$RUN_AS_GROUP $LOCKFILE
            fi
        fi

        # Still want to change users, recurse.  This means that the user will only be
        #  prompted for a password once. Variables shifted by 1
        # 
        # Use "runuser" if this exists.  runuser should be used on RedHat in preference to su.
        #
        if test -f "/sbin/runuser"
        then
            /sbin/runuser - $RUN_AS_USER -c "\"$REALPATH\" $2"
        else
            su - $RUN_AS_USER -c "\"$REALPATH\" $2"
        fi

        # Now that we are the original user again, we may need to clean up the lock file.
        if [ "X$LOCKPROP" != "X" ]
        then
            getpid
            if [ "X$pid" = "X" ]
            then
                # Wrapper is not running so make sure the lock file is deleted.
                if [ -f "$LOCKFILE" ]
                then
                    rm "$LOCKFILE"
                fi
            fi
        fi

        exit 0
    fi
}

getpid() {
    pid=""
    if [ -f "$PIDFILE" ]
    then
        if [ -r "$PIDFILE" ]
        then
            pid=`cat "$PIDFILE"`
            if [ "X$pid" != "X" ]
            then
                # It is possible that 'a' process with the pid exists but that it is not the
                #  correct process.  This can happen in a number of cases, but the most
                #  common is during system startup after an unclean shutdown.
                # The ps statement below looks for the specific wrapper command running as
                #  the pid.  If it is not found then the pid file is considered to be stale.
                case "$DIST_OS" in
                    'macosx')
                        pidtest=`$PSEXE -ww -p $pid -o command | grep "$WRAPPER_CMD_PS" | tail -1`
                        ;;
	                'solaris')
		               if [ -f "/usr/bin/pargs" ]
		               then
		                  pidtest=`pargs $pid | grep "$WRAPPER_CMD" | tail -1`
		               else
		                  case "$PSEXE" in
		                     '/usr/ucb/ps')
		                        pidtest=`$PSEXE -auxww  $pid | grep "$WRAPPER_CMD" | tail -1`
		                        ;;
		                     '/usr/bin/ps')
		                        TRUNCATED_CMD=`$PSEXE -o comm -p $pid | tail -1`
		                        COUNT=`echo $TRUNCATED_CMD | wc -m`
		                        COUNT=`echo ${COUNT}`
		                        COUNT=`expr $COUNT - 1`
		                        TRUNCATED_CMD=`echo $WRAPPER_CMD | cut -c1-$COUNT`
		                        pidtest=`$PSEXE -o comm -p $pid | grep "$TRUNCATED_CMD" | tail -1`
		                        ;;
		                     '/bin/ps')
		                        TRUNCATED_CMD=`$PSEXE -o comm -p $pid | tail -1`
		                        COUNT=`echo $TRUNCATED_CMD | wc -m`
		                        COUNT=`echo ${COUNT}`
		                        COUNT=`expr $COUNT - 1`
		                        TRUNCATED_CMD=`echo $WRAPPER_CMD | cut -c1-$COUNT`
		                        pidtest=`$PSEXE -o comm -p $pid | grep "$TRUNCATED_CMD" | tail -1`
		                        ;;
	                         *)
		                        echo "Unsupported ps command $PSEXE"
		                        exit 1
		                        ;;
		                     esac
		                fi
		                ;;
                    'hpux')
                        pidtest=`$PSEXE -p $pid -x -o args | grep "$WRAPPER_CMD_PS" | tail -1`
                        ;;
                    *)
                        pidtest=`$PSEXE -p $pid -o args | grep "$WRAPPER_CMD_PS" | tail -1`
                        ;;
                esac

                if [ "X$pidtest" = "X" ]
                then
                    # This is a stale pid file.
                    rm -f "$PIDFILE"
                    echo "Removed stale pid file: $PIDFILE"
                    pid=""
                fi
            fi
        else
            echo "Cannot read $PIDFILE."
            exit 1
        fi
    fi
}

getstatus() {
    STATUS=
    if [ -f "$STATUSFILE" ]
    then
        if [ -r "$STATUSFILE" ]
        then
            STATUS=`cat "$STATUSFILE"`
        fi
    fi
    if [ "X$STATUS" = "X" ]
    then
        STATUS="Unknown"
    fi
    
    JAVASTATUS=
    if [ -f "$JAVASTATUSFILE" ]
    then
        if [ -r "$JAVASTATUSFILE" ]
        then
            JAVASTATUS=`cat "$JAVASTATUSFILE"`
        fi
    fi
    if [ "X$JAVASTATUS" = "X" ]
    then
        JAVASTATUS="Unknown"
    fi
}

testpid() {
    # It is possible that 'a' process with the pid exists but that it is not the
    # correct process. This can happen in a number of cases, but the most
    # common is during system startup after an unclean shutdown.
    # The ps statement below looks for the specific wrapper command running as
    # the pid. If it is not found then the pid file is considered to be stale.
    case "$DIST_OS" in
        'macosx')
            pid=`$PSEXE -ww $pid | grep $pid | grep -v grep | awk '{print $1}' | tail -1`
            ;;
    	'solaris')
		   case "$PSEXE" in
		   '/usr/ucb/ps')
		       pid=`$PSEXE  $pid | grep $pid | grep -v grep | awk '{print $1}' | tail -1`
		       ;;
		   '/usr/bin/ps')
		       pid=`$PSEXE -p $pid | grep $pid | grep -v grep | awk '{print $1}' | tail -1`
		       ;;
		   '/bin/ps')
		       pid=`$PSEXE -p $pid | grep $pid | grep -v grep | awk '{print $1}' | tail -1`
		       ;;
		   *)
		       echo "Unsupported ps command $PSEXE"
		       exit 1
		       ;;
		   esac
		    ;;
        *)
            pid=`$PSEXE -p $pid | grep $pid | grep -v grep | awk '{print $1}' | tail -1`
            ;;
    esac
    if [ "X$pid" = "X" ]
    then
        # Process is gone so remove the pid file.
        rm -f "$PIDFILE"
        pid=""
    fi
} 
 
start() {
    $ECHO_NOCR "Starting $APP_LONG_NAME...\c"
    getpid
    if [ "X$pid" = "X" ]
    then
        # The string passed to eval must handles spaces in paths correctly.
        COMMAND_LINE="$CMDNICE \"$WRAPPER_CMD\" \"$WRAPPER_CONF\" \"set.SERVER_INSTALL_HOME=$SERVER_INSTALL_HOME\" \"set.JAVA_HOME=$JAVA_HOME\" wrapper.syslog.ident=\"$APP_NAME\" wrapper.pidfile=\"$PIDFILE\" wrapper.name=\"$APP_NAME\" wrapper.displayname=\"$APP_LONG_NAME\" wrapper.daemonize=TRUE $ANCHORPROP $IGNOREPROP $STATUSPROP $LOCKPROP"
        eval $COMMAND_LINE
    else
        echo "$APP_LONG_NAME is already running."
        exit 1
    fi

    # Sleep for a few seconds to allow for intialization if required 
    #  then test to make sure we're still running.
    #
    i=0
    while [ $i -lt $WAIT_AFTER_STARTUP ]
    do
        sleep 1
        $ECHO_NOCR ".\c"
        i=`expr $i + 1`
    done
    if [ $WAIT_AFTER_STARTUP -gt 0 ]
    then
        getpid
        if [ "X$pid" = "X" ]
        then
            echo " WARNING: $APP_LONG_NAME may have failed to start."
            exit 1
        else
            echo " running ($pid)."
        fi
    else 
        echo ""
    fi
}
 
stopit() {
    echo "Stopping $APP_LONG_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_LONG_NAME was not running."
    else
        if [ "X$IGNORE_SIGNALS" = "X" ]
        then
            # Running so try to stop it.
            kill $pid
            if [ $? -ne 0 ]
            then
                # An explanation for the failure should have been given
                echo "Unable to stop $APP_LONG_NAME."
                exit 1
            fi
        else
            rm -f "$ANCHORFILE"
            if [ -f "$ANCHORFILE" ]
            then
                # An explanation for the failure should have been given
                echo "Unable to stop $APP_LONG_NAME."
                exit 1
            fi
        fi

        # We can not predict how long it will take for the wrapper to
        #  actually stop as it depends on settings in wrapper.conf.
        #  Loop until it does.
        savepid=$pid
        CNT=0
        TOTCNT=0
        while [ "X$pid" != "X" ]
        do
            # Show a waiting message every 5 seconds.
            if [ "$CNT" -lt "5" ]
            then
                CNT=`expr $CNT + 1`
            else
                echo "Waiting for $APP_LONG_NAME to exit..."
                CNT=0
            fi
            TOTCNT=`expr $TOTCNT + 1`

            sleep 1

            testpid
        done

        pid=$savepid
        testpid
        if [ "X$pid" != "X" ]
        then
            echo "Failed to stop $APP_LONG_NAME."
            exit 1
        else
            echo "Stopped $APP_LONG_NAME."
        fi
    fi
}

status() {
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_LONG_NAME is not running."
        exit 1
    else
        if [ "X$DETAIL_STATUS" = "X" ]
        then
            echo "$APP_LONG_NAME is running (PID:$pid)."
            ${STATUS_CMD}
        else
            getstatus
            echo "$APP_LONG_NAME is running (PID:$pid, Wrapper:$STATUS, Java:$JAVASTATUS)"
            ${STATUS_CMD}
        fi
        exit 0
    fi
}

dump() {
    echo "Dumping $APP_LONG_NAME..."
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "$APP_LONG_NAME was not running."
    else
        kill -3 $pid

        if [ $? -ne 0 ]
        then
            echo "Failed to dump $APP_LONG_NAME."
            exit 1
        else
            echo "Dumped $APP_LONG_NAME."
        fi
    fi
}

# Used by HP-UX init scripts.
startmsg() {
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "Starting $APP_LONG_NAME... (Wrapper:Stopped)"
    else
        if [ "X$DETAIL_STATUS" = "X" ]
        then
            echo "Starting $APP_LONG_NAME... (Wrapper:Running)"
        else
            getstatus
            echo "Starting $APP_LONG_NAME... (Wrapper:$STATUS, Java:$JAVASTATUS)"
        fi
    fi
}

# Used by HP-UX init scripts.
stopmsg() {
    getpid
    if [ "X$pid" = "X" ]
    then
        echo "Stopping $APP_LONG_NAME... (Wrapper:Stopped)"
    else
        if [ "X$DETAIL_STATUS" = "X" ]
        then
            echo "Stopping $APP_LONG_NAME... (Wrapper:Running)"
        else
            getstatus
            echo "Stopping $APP_LONG_NAME... (Wrapper:$STATUS, Java:$JAVASTATUS)"
        fi
    fi
}


case "$1" in

    'start')
        checkUser touchlock $1
        start
        ;;

    'stop')
        checkUser "" $1
        stopit
        ;;

    'restart')
        checkUser touchlock $1
        stopit
        start
        ;;

    'status')
        checkUser "" $1
        status
        ;;

    'dump')
        checkUser "" $1
        dump
        ;;

    'start_msg')
        checkUser "" $1
        startmsg
        ;;

    'stop_msg')
        checkUser "" $1
        stopmsg
        ;;

    *)
        echo "Usage: $0 { start | stop | restart | status | dump }"
        exit 1
        ;;
esac

exit 0
