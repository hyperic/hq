#!/bin/bash
. etc/multiagent.properties

CLIENT_CMD="${JAVA_HOME}/bin/java -client \
        -Dagent.propFile=agent.properties \
        -Dorg.hyperic.hq.bizapp.agent.CommandsAPIInfo.camUpPort=111 \
        -Dagent.install.home=$AGENT_HOME \
        -Dagent.bundle.home=$AGENT_BUNDLE_HOME \
        -Dagent.mode=thread \
        -Djava.security.auth.login.config=jaas.config \
        -Djava.net.preferIPv4Stack=true \
        -Dcom.sun.management.jmxremote \
        $CLONE_JAVA_FLAGS \
        -cp $CLONE_CP org.hyperic.util.thread.MultiRunner $@"

echo $CLIENT_CMD

echo "***** the start time is "
echo `date`

nohup $CLIENT_CMD 2>&1 > multiagent.log &
pid=$!
echo ${pid} > multiagent.pid

echo "***** the end time is "
echo `date`

