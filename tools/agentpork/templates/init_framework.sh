#!/bin/sh

BASE_DIR=$1

cat etc/multiagent.properties.template | \
    sed -e  s:@GRP_ID@:$GROUP_ID:g  | \
    sed -e  s:@CLONES_PER_GROUP@:$CLONES_PER_GROUP:g  | \
    sed -e  s:@NUM_CLONES@:$CLONES_PER_GROUP:g  | \
    sed -e  s:@BASE_DIR@:${BASE_DIR}:g > etc/multiagent.properties

. etc/multiagent.properties

function download_agent {
if [ ! -d $AGENT_BUNDLE_HOME ]
then
   cdir=$PWD
   mkdir -p $AGENT_BASE_DIR
   cd $AGENT_BASE_DIR
   wget http://private.maven.hyperic.com/snapshot/com/hyperic/hq/dist/hyperic-hqee-agent/$AGENT_BUILD_VERSION.BUILD-SNAPSHOT/hyperic-hqee-agent-$AGENT_BUILD_VERSION.BUILD-$AGENT_BUILD_NUMBER-noJRE.tar.gz
   if [ -d $AGENT_HOME ]
   then
     rm -rf $AGENT_HOME
   fi
   tar -zxf hyperic-hqee-agent-$AGENT_BUILD_VERSION.BUILD-$AGENT_BUILD_NUMBER-noJRE.tar.gz
   cp ../testdata-plugin.jar $AGENT_BUNDLE_HOME/pdk/plugins
   cd $cdir
fi
}    

download_agent

# Ask User to copy the plugin manually to server. Wait for user confirmation

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
    sed -e  s:@AGENT_BUILD_VERSION@:${AGENT_BUILD_VERSION}:g | \
    sed -e  s:@NUM_CLONES@:${NUM_CLONES}:g > clone.properties

