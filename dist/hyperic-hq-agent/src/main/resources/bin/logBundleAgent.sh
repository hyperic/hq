#!/bin/bash

# Make sure only root can run our script
#if [[ $EUID -ne 0 ]]; then
#   echo "This script must be run as root" 1>&2
#   exit 1
#fi

EXECUTION_DIR=`pwd`

# Get Hyperic dir
cd `dirname $0`
INSTALLBASE=`pwd`
echo $INSTALLBASE

# Location of Hyperic agent logs.
LOGS_DIR="../log"
CONF_DIR="../conf"
STAT_DIR="../log/agentstats"

PRODUCT_HOME="/opt"
DATE=`date "+%Y%m%d_%H%M"`
PLATNAME=`hostname`

SP_DIR_NAME="hyperic_agent_SP_""$PLATNAME"_"$DATE"
SP_TMP_DIR="/tmp/$SP_DIR_NAME"
SP_NAME="$SP_DIR_NAME.tgz"
SP_TMP_DIR_AGENT_LOGS=$SP_TMP_DIR/agent-logs
SP_TMP_DIR_AGENT_CONF=$SP_TMP_DIR/agent-conf
SP_TMP_DIR_AGENT_STAT=$SP_TMP_DIR/agent-stat



SP_ERROR_LOG="$SP_TMP_DIR/supportpackage.log"

# file to copy; Format of FILES is "fileToCopy1,targetLocation1 fileToCopy2,targetLocation2 ... fileToCopyN,targetLocationN"
FILES="
$LOGS_DIR/wrapper.log,$SP_TMP_DIR_AGENT_LOGS
$LOGS_DIR/agent.log,$SP_TMP_DIR_AGENT_LOGS
$CONF_DIR/rollback.properties,$SP_TMP_DIR_AGENT_CONF
$CONF_DIR/agent.properties,$SP_TMP_DIR_AGENT_CONF
$CONF_DIR/auto-approve.properties,$SP_TMP_DIR_AGENT_CONF
"

echo ""
echo Creating tmp directories...
mkdir $SP_TMP_DIR
mkdir $SP_TMP_DIR_AGENT_LOGS
mkdir $SP_TMP_DIR_AGENT_CONF
mkdir $SP_TMP_DIR_AGENT_STAT
#cd $SP_TMP_DIR



echo ""
echo Copying files...
for file in $FILES; do
        set -- `echo $file | tr , \ `
        cp -v -r $1 $2 2>>$SP_ERROR_LOG
done

# Copying HQ Stats separately as wildcard was not working in loop.  Wild card needed to get current uncompressed csv file(s)
echo ""
echo Copying HQ Stats...
cp $STAT_DIR/agentstats-*.csv $SP_TMP_DIR_AGENT_STAT

echo ""
echo Copying agent-diag-Day log files...
cp $LOGS_DIR/agent-*.gz $SP_TMP_DIR_AGENT_LOGS
 

cd $SP_TMP_DIR
echo ""
echo Compressing and packaging data...
cd ..
tar czf $SP_NAME $SP_DIR_NAME
mv $SP_NAME $EXECUTION_DIR
cd $EXECUTION_DIR

echo ""
echo Cleaning up...
rm -rf $SP_TMP_DIR

echo ""
echo Created agent support package: $SP_NAME
exit

