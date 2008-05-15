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
  chmod -R +x ./bundles/$AGENT_BUNDLE/bin/*
  # pass on the command to the bundle
  #echo "Invoking agent bundle $AGENT_BUNDLE"
  ./bundles/$AGENT_BUNDLE/bin/hq-agent.sh "$@"
else
  echo "Bundle $AGENT_BUNDLE does not exist!"
fi
