#!/bin/bash

. etc/group.properties

# Create and configure clone group dir structure
# runtimeGroups/1, runtimeGroups/2 ...

NUM_GROUPS=$(($TOTAL_CLONES / $CLONES_PER_GROUP))
LAST_CLONE_ID=$(($CLONES_PER_GROUP - 1))
BASE_DIR=$PWD

if [ "x${HQ_JAVA_HOME}" != "x" ] ; then
    echo "Using HQ_JAVA_HOME = ${HQ_JAVA_HOME}"
elif [ "x${JAVA_HOME}" != "x" ] ; then
    HQ_JAVA_HOME=${JAVA_HOME}
    echo "Using HQ_JAVA_HOME = $JAVA_HOME"
else
        echo "HQ_JAVA_HOME or JAVA_HOME must be set when invoking multiagent"
        exit 1
fi


function wantToContinue  {
read -p  "Want to continue? (y/n)" A
if [ $A = "n" ]
then
    exit 1
fi
}

echo "==== STARTING AND APPROVING MULTI AGENT ===="

# Start multiagents and approve
for ((i=1;i<=$NUM_GROUPS;i++))
do
  if [ ! -d runtimeGroups/$i ]
  then
    echo "Tried to start a group runtimeGroups/$i, but can't find it."
    echo "Check user permissions, run wipeMultiagents.sh and then startMultiagent.sh"
    exit
  fi
  cd runtimeGroups/$i
  echo "Starting clones in group $i"
  ./start_multiagent.sh clone.properties 
  sleep 90
  #echo "Approving clones in group $i"
  #nohup ./approve_agents.sh  $i 2>&1 > approve_agents.log &
  cd ../..
done

