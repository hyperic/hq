To run multiagent

1. Update etc/group.properties
2. Update following properties in templates/etc/multiagent.properties.template
      
      # Agent release number
      export AGENT_BUILD_VERSION=4.5.0 
      # Agent Build number (multiagent works with SNAPSHOT builds only)
      export AGENT_BUILD_NUMBER=20100806.095723-67 
      # HQ server IP or hostname
      export CLONE_SERVERIP=vmc-ssrc-rh34 
      # Update following based depending on how many agents (clones) you want to run, following is good for running 50 agents.
      export CLONE_JAVA_FLAGS="-XX:MaxPermSize=192m -Xmx256m -Xms256m -XX:+UseConcMarkSweepGC" 

3. Set HQ_JAVA_HOME or JAVA_HOME environment variable
4. Deploy following plugin on your HQ server, this plain is available under root dir of multiagent.zip
      testdata-plugin.jar
5. Check your HQ server license. Change it to dev unlimited if you are running more then 50 agents
6. ./bootstrapMultiagent.sh 
