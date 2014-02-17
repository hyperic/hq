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

# Location of Hyperic server logs.
LOGS_DIR="../logs"
CONF_DIR="../conf"
STAT_DIR="../hq-engine/hq-server/logs/hqstats"
WEB_INF="../hq-engine/hq-server/webapps/ROOT/WEB-INF"

PRODUCT_HOME="/opt"
DATE=`date "+%Y%m%d_%H%M"`
PLATNAME=`hostname`
echo "$PLATNAME"

SP_DIR_NAME="hyperic_server_SP_""$PLATNAME"_"$DATE"
SP_TMP_DIR="/tmp/$SP_DIR_NAME"
SP_NAME="$SP_DIR_NAME.tgz"
SP_TMP_DIR_SERVER_LOGS=$SP_TMP_DIR/server-logs
SP_TMP_DIR_SERVER_CONF=$SP_TMP_DIR/server-conf
SP_TMP_DIR_SERVER_STAT=$SP_TMP_DIR/server-stat
SP_TMP_DIR_SERVER_WEB_INF=$SP_TMP_DIR/WEB-INF


SP_ERROR_LOG="$SP_TMP_DIR/supportpackage.log"

# file to copy; Format of FILES is "fileToCopy1,targetLocation1 fileToCopy2,targetLocation2 ... fileToCopyN,targetLocationN"
FILES="
$LOGS_DIR/wrapper.log,$SP_TMP_DIR_SERVER_LOGS
$LOGS_DIR/server.log,$SP_TMP_DIR_SERVER_LOGS
$LOGS_DIR/bootstrap.log,$SP_TMP_DIR_SERVER_LOGS
$CONF_DIR/wrapper.conf,$SP_TMP_DIR_SERVER_CONF
$CONF_DIR/hq-server.conf,$SP_TMP_DIR_SERVER_CONF 
$CONF_DIR/server-log4j.xml,$SP_TMP_DIR_SERVER_CONF
$CONF_DIR/log4j.xml,$SP_TMP_DIR_SERVER_CONF
$WEB_INF/classes/ehcache.xml,$SP_TMP_DIR_SERVER_WEB_INF
$WEB_INF/classes/ApplicationResources.properties,$SP_TMP_DIR_SERVER_WEB_INF
"

echo $SP_TMP_DIR_SERVER_STAT

echo ""
echo Creating tmp directories...
mkdir $SP_TMP_DIR
mkdir $SP_TMP_DIR_SERVER_LOGS
mkdir $SP_TMP_DIR_SERVER_CONF
mkdir $SP_TMP_DIR_SERVER_STAT
mkdir $SP_TMP_DIR_SERVER_WEB_INF
#cd $SP_TMP_DIR



echo ""
echo Copying files...
for file in $FILES; do
        set -- `echo $file | tr , \ `
        cp -v -r $1 $2 2>>$SP_ERROR_LOG
done

# Copying HQ Stats separately as wildcard was not working in loop
echo ""
echo Copying Stats...
cp $STAT_DIR/hqstats-*.csv $SP_TMP_DIR_SERVER_STAT

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
echo Created server support package: $SP_NAME
exit

