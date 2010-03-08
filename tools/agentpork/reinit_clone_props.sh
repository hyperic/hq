#!/bin/sh

. etc/multiagent.properties

cat etc/clone.properties | \
    sed -e  s:@AGENT_BUNDLE_HOME@:${AGENT_BUNDLE_HOME}:g | \
    sed -e  s:@NUM_CLONES@:${NUM_CLONES}:g > clone.properties

