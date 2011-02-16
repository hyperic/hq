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

# Initialize multi agent directory structure

echo "==== INITIALIZING MULTI AGENT ===="
for ((i=1;i<=$NUM_GROUPS;i++))
do
  # Exit if group dir exists
  if [ -d runtimeGroups/$i ]
  then
    echo "Group dir runtimeGroups/$i exists, run wipeMultiagents.sh to cleanup"
    exit
  fi
  mkdir -p runtimeGroups/$i
  cp -r templates/* runtimeGroups/$i
  cd runtimeGroups/$i
  export GROUP_ID=$i
  ./init_framework.sh $BASE_DIR
  cd ../..
done


echo "==== CLONING MULTI AGENT ===="



# Clone agents

for ((i=1;i<=$NUM_GROUPS;i++))
do
  if [ ! -d runtimeGroups/$i ]
  then
    echo "Tried to start a group runtimeGroups/$i, but can't find it. Check user permissions, run wipeMultiagents.sh and then startMultiagent.sh"
    exit
  fi
  cd runtimeGroups/$i
  nohup ./forkagents.sh $i &
  sleep 120
  cd ../..
done

echo "Waiting for clones to initialize."
echo "This may take several minutes depending on number of clones you are creating"
echo -n "..."

while [ 1 ]
do
  if [ ! -d runtimeGroups/$NUM_GROUPS/clones/clone_${LAST_CLONE_ID}/data ]
  then
    echo -n "."
  else
    break
  fi
  sleep 10
done


# Get hqapi
if [ ! -d hqapi1-3.1 ]
then
  wget http://10.0.0.58/raid/release/candidates.restore/qatools/hqapi1-3.1.tar.gz
  tar -zxf hqapi1-3.1.tar.gz
fi

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
  ln -s ../../hqapi1-3.1 hqapi
  echo "Starting clones in group $i"
  ./start_multiagent.sh clone.properties 
  sleep 60
  echo "Approving clones in group $i"
  nohup ./approve_agents.sh  $i 2>&1 > approve_agents.log &
  cd ../..
done

