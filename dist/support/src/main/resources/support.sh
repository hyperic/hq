#!/bin/bash

# This script runs the support frontend. The actual code resides in scripts/support.py in order to be
# more platform independent. 
#
# All parameters are passed directly to the support.py script
#
# NOTE: In order to prevent any dependency on python installations, this code runs the python script
#       using Jython

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

INSTALL_HOME="`dirname "$REALPATH"`/.."

JAVA_EXECUTABLE="$INSTALL_HOME/jre/bin/java"

if [ ! -f $JAVA_EXECUTABLE ] ; then
    JAVA_HOME_EXECUTABLE="$JAVA_HOME/bin/java"
    if [ ! -f $JAVA_HOME_EXECUTABLE ] ; then
        echo "Could not find java executable in installation JRE and JAVA_HOME is not defined. Cannot run"
    else
        JAVA_EXECUTABLE="$JAVA_HOME_EXECUTABLE"
    fi
fi

JYTHON_LIB="$INSTALL_HOME/support/lib/jython.jar"

if [ ! -f $INSTALL_HOME/support/scripts/support.py ] ; then
    # Probably a dev machine
    SUPPORT_SRC_MAIN_RESOURCES="`dirname "$REALPATH"`"
    if [ ! -f $SUPPORT_SRC_MAIN_RESOURCES/scripts/support.py ] ; then
        echo "Could not find support.py in the proper location. This is probably not a dev machine"
    else
        JYTHON_LIB="$SUPPORT_SRC_MAIN_RESOURCES/lib/jython.jar"
        eval $JAVA_EXECUTABLE -jar $JYTHON_LIB $SUPPORT_SRC_MAIN_RESOURCES/scripts/support.py "$@"
    fi    
else
    eval $JAVA_EXECUTABLE -jar $JYTHON_LIB $INSTALL_HOME/support/scripts/support.py "$@"
fi


