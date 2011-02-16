To run multiagent

1. Update etc/group.properties
2. Update following properties in templates/etc/multiagent.properties.template
      
      # Agent release number
      export AGENT_BUILD_VERSION=4.5.0 
      # Agent Build number
      export AGENT_BUILD_NUMBER=20100806.095723-67 
      # HQ server IP
      export CLONE_SERVERIP=vmc-ssrc-rh34 
      # Only update if you are running more then 50 clones
      export CLONE_JAVA_FLAGS="-XX:MaxPermSize=192m -Xmx256m -Xms256m" 

3. Set HQ_JAVA_HOME or JAVA_HOME environment variable
4. Deploy following plugin on your HQ server
      http://10.0.0.240/raid/release/candidates/qatools/mbeanserver/plugins/jmx-testdata-plugin.xml
5. Check your HQ server license. Change it to dev unlimited if you are running
more then 50 agents
6. ./bootstrapMultiagent.sh 
