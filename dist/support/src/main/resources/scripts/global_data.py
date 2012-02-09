#/usr/bin/python

# This module maintains a set of variables, along with their descriptions

import os,sys
import logging

logger = logging.getLogger('global_data')

# variable map
globalData = {}

# Constants for variable names
SCRIPTS_PATH = 'scripts_path'
BASE_WORKING_FOLDER = 'base_working_folder'
SUPPORT_PACKAGE_NAME = 'support_package_name'
SUPPORT_PACKAGE_FULL_PATH = 'support_package_full_path'
DETAIL_WORKING_FOLDER = 'detail_working_folder'
ARCH = 'arch'
INSTALLATION_TYPE = 'installation_type'
HQ_HOME = 'hq_home'
SERVER_HOME = 'server_home'
SERVER_JRE = 'server_jre'
AGENT_HOME = 'agent_home'
AGENT_JRE = 'agent_jre'
JRE = 'jre'
JAVA_EXECUTABLE = 'java_executable'
SUPPORT_BASE = 'support_base'
SUPPORT_PROJECT_BASE = 'support_project_base'
LIB_FOLDER = 'lib_folder'
JYTHON_JAR_LOCATION = 'jython_jar_location'
RUN_JYTHON = 'run_jython'
SIMPLE_TAR = 'simple_tar'
SIGAR_JAR = 'sigar_jar'
RUN_SIGAR = 'run_sigar'

# List of global data descriptions
globalDataRepository = { SCRIPTS_PATH : 'Path to all scripts. Use %(scripts_path)s/%(arch)s/script_name to access the script for a specific architecture',
                         BASE_WORKING_FOLDER : 'Target folder of final archive',
                         DETAIL_WORKING_FOLDER : 'Work area for the actual files. Script should write any extra files to that folder',
                         SUPPORT_PACKAGE_NAME : 'The filename of the resulting support package',
                         SUPPORT_PACKAGE_FULL_PATH : 'The absolute filename of the resulting support package',
                         ARCH : "The system's architecture",
                         INSTALLATION_TYPE : 'The type of the application installation - server, agent or dev',
                         HQ_HOME : "Home folder of HQ, whether it's a server or an agent",
                         SERVER_HOME : 'The home folder of the server, if it is a server machine',
                         AGENT_HOME : 'The home folder of the agent, if it is an agent machine',
                         SERVER_JRE : 'The JRE folder of the server, if it is a server machine',
                         AGENT_JRE : 'The JRE folder of the agent, if it is an agent machine',
                         JRE : 'Path to Java JRE',
                         JAVA_EXECUTABLE : 'Path to Java Executable',
                         SUPPORT_BASE : 'Support base folder. Only for server/agent installations',
                         SUPPORT_PROJECT_BASE : 'Base folder for support source code project. Only for dev installations',
                         LIB_FOLDER : 'Path to lib folder (where jython.jar resides)',
                         JYTHON_JAR_LOCATION : 'Full path of jython.jar',
                         RUN_JYTHON : 'Command line to execute jython',
                         SIMPLE_TAR : 'Command line to run simpletar.py - a simplistic platform independent tar utility' ,
                         RUN_SIGAR : 'Command line to run a sigar command',
                         SIGAR_JAR : 'Get the jar location of sigar'
}


# Add a new variable with its value
def addGlobalData(name,value):
    global globalData

    if not name in globalDataRepository.keys():
        raise Exception('Global data %s does not exist' % name)

    globalData[name] = value
    logger.debug("Setting global data variable %s to '%s'" % (name,value))

# Get a variable's value
def getGlobalData(name):
    global globalData
    if not name in globalDataRepository.keys():
        raise Exception('Global data cannot hold entries that have a name of %s' % name)

    if not name in globalData.keys():
        raise Exception('Could find requested global variable')

    return globalData[name]

# List all variables and their descriptions
def listGlobalDataRepository():
    for k,v in globalDataRepository.items():
        print "%-35s\t\t%-40s" % ("%("+k+")s",v)
