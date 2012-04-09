#!/usr/bin/env python
 
import os
import tarfile
import zipfile
import fileinput, sys
import socket
from ProviderFramework.Util.Logging.Logging import logger
from ProviderFramework.DataClasses.DataClass import data_class
from ProviderFramework.DataClasses.DataClass import field
from ProviderFramework.DataClasses.BasicDataClasses import Information
from ProviderFramework.DataClasses.DataTypes import DataTypes
from ProviderFramework.ActionClasses.ActionClass import action_class
from ProviderFramework.ActionClasses.ActionClass import collect_method
from ProviderFramework.ActionClasses.ActionClass import method
from ProviderFramework.ActionClasses.ActionClass import parameter
from ProviderFramework.ActionClasses.ActionClass import instance_parameter
from ProviderFramework.Provider.Provider import provider
from ProviderFramework.Provider.ProviderDriver import ProviderDriver
from ProviderFramework.Provider.ProviderContext import ProviderContext

@action_class(version='1.0')
class AgentDeployer(object):

	@method(returns=Information)
	@parameter(
		    name='path_to_agent_file', data_type=DataTypes.String, required=True,
		    description='The path to the agents file', display_name='path_to_agent_file')
	@parameter(
		    name='file_name', data_type=DataTypes.String, required=True,
		    description='The name of the agents file',
		    display_name='file_name')
	@parameter(
		    name='to_directory', data_type=DataTypes.String, required=True,
		    description='The directory to install the agent',
		    display_name='to_directory')	
	@parameter(
		    name='ip', data_type=DataTypes.String, required=True,
		    description='The IP of the server',
		    display_name='ip')
	@parameter(
		    name='port', data_type=DataTypes.String, required=False,
		    default='7080', 
		    description='The Hyperic server non-secure port',
		    display_name='port')
	@parameter(
		    name='secure_port', data_type=DataTypes.String, required=False,
		    default='7443', 
		    description='The Hyperic server secure port',
		    display_name='secure_port')
	@parameter(
		    name='username', data_type=DataTypes.String, required=False,
		    default='hqadmin', 
		    description='The Hyperic server admin username',
		    display_name='username')
	@parameter(
		    name='password', data_type=DataTypes.String, required=False,
		    default='hqadmin', 
		    description='The Hyperic server admin password',
		    display_name='password')
	def install_agent(self, path_to_agent_file, file_name, to_directory, ip, port, secure_port, username, password):
	    
	    #check if port 2144 is used, if so fail - probably means that Hyperic agent is already running on this machine
 	    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    	    try:
     	    	s.connect(('localhost', int(2144)))
     	    	s.shutdown(2)
     	    	msg = "Port 2144 is used, this probably means that Hyperic agent is already running on this machine" 
	    	sys.stderr.write(msg)
	    	formatter = ProviderContext.get_formatter()
	    	formatter.format(Information(msg, 'Informational'))
		return
   	    except:
     	    	logger.info("Port 2144 is available");

	    #make sure that all the path variables ends with '/'
	    if not path_to_agent_file.endswith('/'):
           	path_to_agent_file += '/'
            if not to_directory.endswith('/'):
           	to_directory += '/'
		
	    #determin the type of extractor according to the file name
	    if file_name.endswith('.zip'):
        	opener, mode = zipfile.ZipFile, 'r'
	    if file_name.endswith('.tar.gz') or file_name.endswith('.tgz'):
		  opener, mode = tarfile.open, 'r:gz'
	    elif file_name.endswith('.tar.bz2') or file_name.endswith('.tbz'):
		  opener, mode = tarfile.open, 'r:bz2'
	    else: 
	    	  msg = "Could not extract the agent's file as no appropriate extractor is found" 
	    	  sys.stderr.write(msg)
	    	  formatter = ProviderContext.get_formatter()
	    	  formatter.format(Information(msg, 'Informational'))
	          return
	    
	    #check if this agent is already installed on this machine
	    if os.path.exists(to_directory + file_name[0:file_name.find("-noJRE")]):
		msg =  file_name[0:file_name.find("-noJRE")] + " is already installed on this machine"
	    	sys.stderr.write(msg)
	    	formatter = ProviderContext.get_formatter()
	    	formatter.format(Information(msg, 'Informational'))
		return

	    #create the directory and extract the agent
	    if not os.path.exists(to_directory):
		        os.makedirs(to_directory)
	    cwd = os.getcwd()
	    os.chdir(to_directory)
	    
	    try:
		file = opener(path_to_agent_file + file_name, mode)
		try: file.extractall()
		finally: file.close()
	    finally:
		os.chdir(cwd)

	    #change the agent's properties file and start the agent
	    agent_name = file_name[0:file_name.find("-noJRE")] 
	    properties_file = to_directory + agent_name + "/conf/agent.properties"
	    start_agent = to_directory + agent_name + "/bin/hq-agent.sh start"
	  
	    for line in fileinput.input([properties_file],inplace=True):
	 	   line=line.replace("#agent.setup.camIP=localhost","agent.setup.camIP=" + ip)
		   line=line.replace("#agent.setup.camPort=7080","agent.setup.camPort=" + port)   
		   line=line.replace("#agent.setup.camSSLPort=7443","agent.setup.camSSLPort=" + secure_port)
	 	   line=line.replace("#agent.setup.camSecure=yes" ,"agent.setup.camSecure=yes")
		   line=line.replace("#agent.setup.camLogin=hqadmin" , "agent.setup.camLogin=" + username)
		   line=line.replace("#agent.setup.camPword=hqadmin" , "agent.setup.camPword=" + password)
		   line=line.replace("#agent.setup.agentIP=*default*" , "agent.setup.agentIP=*default*")
		   line=line.replace("#agent.setup.agentPort=*default*" , "agent.setup.agentPort=*default*")
		   line=line.replace("#agent.setup.resetupTokens=no" , "agent.setup.resetupTokens=no")
		   line=line.replace("#agent.setup.acceptUnverifiedCertificate=no" , "agent.setup.acceptUnverifiedCertificate=yes")
		   line=line.replace("accept.unverified.certificates=false" , "accept.unverified.certificates=true")
		   line=line.replace("#agent.setup.unidirectional=no" , "agent.setup.unidirectional=no")
	           sys.stdout.write(line)
	  
	    os.system(start_agent)
	    formatter = ProviderContext.get_formatter()
	    formatter.format(Information("Started the agent", 'Informational'))
	
	@method(returns=Information)
	@parameter(
		    name='agent_directory', data_type=DataTypes.String, required=True,
		    description='The path to the agents directory', display_name='agent_directory')
	def start_agent(self, agent_directory):
	     if not agent_directory.endswith('/'):
           	agent_directory += '/'
             os.system(agent_directory + 'bin/hq-agent.sh start')
             formatter = ProviderContext.get_formatter()
	     formatter.format(Information("", 'Informational'))

	@method(returns=Information)
	@parameter(
		    name='agent_directory', data_type=DataTypes.String, required=True,
		    description='The path to the agents directory', display_name='agent_directory')
	def stop_agent(self, agent_directory):
	     if not agent_directory.endswith('/'):
           	agent_directory += '/'
             os.system(agent_directory + 'bin/hq-agent.sh stop')
	     formatter = ProviderContext.get_formatter()
	     formatter.format(Information("", 'Informational'))

	@method(returns=Information)
	@parameter(
		    name='agent_directory', data_type=DataTypes.String, required=True,
		    description='The path to the agents directory', display_name='agent_directory')
	def restart_agent(self, agent_directory):
	     if not agent_directory.endswith('/'):
           	agent_directory += '/'
	     os.system(agent_directory + 'bin/hq-agent.sh restart')
	     formatter = ProviderContext.get_formatter()
	     formatter.format(Information("", 'Informational'))

@provider(version='1.0', namespace='Hyperic', actions=[AgentDeployer])
class HypericAgentDeployerProvider(object):
    pass
 
def main():
    hyperic_deployer_provider = ProviderDriver(HypericAgentDeployerProvider)
    hyperic_deployer_provider.main()
 
if __name__ == '__main__':
    main()


