#!/bin/sh

# find the current path to master executable
FINDNAME=$0 
while [ -h $FINDNAME ] ; do FINDNAME=`ls -ld $FINDNAME | awk '{print $NF}'` ; done 
RUNDIR=`echo $FINDNAME | sed -e 's@/[^/]*$@@'` 
unset FINDNAME

# cd to top level agent home
if test -d $RUNDIR; then
  cd $RUNDIR/..
else
  cd ..
fi

ROLLBACK_PROPERTIES=conf/rollback.properties
PROP_NAME=set.HQ_AGENT_BUNDLE

# resolve the HQ Agent Bundle property
AGENT_BUNDLE=`grep $PROP_NAME $ROLLBACK_PROPERTIES | awk -F= '{print $2}'`

if test -d "./bundles/$AGENT_BUNDLE"; then
  # be safe and set permissions for the invoked script
  chmod -R +x ./bundles/$AGENT_BUNDLE/bin/* > /dev/null 2>&1
  # Fix permissions issues on HPUX
  chmod -R +x ./bundles/$AGENT_BUNDLE/pdk/lib/*.sl > /dev/null 2>&1
  #also for backround.sh which is in different folder
  chmod  +x ./bundles/$AGENT_BUNDLE/* > /dev/null 2>&1
  # pass on the command to the bundle
  #echo "Invoking agent bundle $AGENT_BUNDLE"
  if [ $1 = "set-property" ]
  then
    ./bundles/$AGENT_BUNDLE/bin/hq-agent-nowrapper.sh "$@"
  else
    ./bundles/$AGENT_BUNDLE/bin/hq-agent.sh "$@"
  fi
else
  echo "Bundle $AGENT_BUNDLE does not exist!"
fi
