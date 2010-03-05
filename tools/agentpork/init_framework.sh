#!/bin/sh

. etc/multiagent.properties

mkdir clones
cd clones
ln -s $AGENT_BUNDLE_HOME master
ln -s $AGENT_HOME/wrapper wrapper
ln -s master/lib lib
ln -s master/pdk pdk

cd ..
ln -s clones/pdk pdk
ln -s clones/lib lib

cat etc/clone.properties | \
    sed -e  s:@AGENT_BUNDLE_HOME@:${AGENT_BUNDLE_HOME}:g | \
    sed -e  s:@NUM_CLONES@:${NUM_CLONES}:g > clone.properties

 
