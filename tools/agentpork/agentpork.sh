CLONE_JAVA_FLAGS=""
. agentpork.cfg

${JAVA_HOME}/bin/java \
-client \
-Dagent.propFile=agent.properties \
-Dorg.hyperic.hq.bizapp.agent.CommandsAPIInfo.camUpPort=111 \
-Djava.security.auth.login.config=jaas.config \
-Djava.net.preferIPv4Stack=true \
-XX:MaxPermSize=256m \
$CLONE_JAVA_FLAGS \
-classpath clones/pdk/lib/hyperic-util.jar:clones/pdk/lib/log4j-1.2.14.jar:clones/pdk/lib/ant.jar:clones/pdk/lib/jdom_b8.jar:clones/pdk/lib/commons-logging.jar:clones/pdk/lib/commons-httpclient-2.0.jar:clones/pdk/lib/sigar.jar \
org.hyperic.util.thread.MultiRunner  $1

#-Xdebug \
#-Xrunjdwp:transport=dt_socket,server=y,suspend=y \
