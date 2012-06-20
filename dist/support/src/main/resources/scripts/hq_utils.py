#!/usr/bin/python
#
# This file is used for detecting the HQ installation type and the system's architecture.
#
# If the support-package script needs to be used for other projects, then this file needs to be different 
# (and possibly more generic). Other files are mostly application independent.
#

import os,sys,tempfile,glob,time,shutil
from subprocess import call
import global_data as gd
from global_data import addGlobalData
import logging

logger = logging.getLogger('hq_util')

SERVER = "server"
AGENT = "agent"
DEV = 'dev'

# Detect whether we're on an HQ server, HQ agent or a dev machine
def getInstallationType(hq_home):
    if os.path.exists(os.path.join(hq_home,'hq-engine')) and os.path.exists(os.path.join(hq_home,'wrapper')):
        return SERVER
    else:
        if os.path.exists(os.path.join(hq_home,'bundles')) and os.path.exists(os.path.join(hq_home,'wrapper')):
            return AGENT
        else:
            if os.path.exists(os.path.join(hq_home,'resources')):
                return DEV
            else:
                raise Exception('Unknown HQ environment')

# Detect HQ related information, base on the HQ home folder
def detectHQInformation(hq_home):
    # Add hq home as a a variable
    addGlobalData(gd.HQ_HOME,hq_home)

    # Detect the HQ installation type
    installation_type = getInstallationType(hq_home)
    addGlobalData(gd.INSTALLATION_TYPE,installation_type)
    # If it's a server installation,
    if installation_type == SERVER:
        addGlobalData(gd.SERVER_HOME,hq_home)
        expected_jre_location = os.path.join(hq_home,"jre")
        if os.path.exists(expected_jre_location):
            addGlobalData(gd.SERVER_JRE,expected_jre_location)
            addGlobalData(gd.JRE,expected_jre_location)
        else:
            # Add JAVA_HOME as the JRE path
            if 'JAVA_HOME' in os.environ.keys():
                addGlobalData(gd.JRE,os.environ['JAVA_HOME'])
        addGlobalData(gd.SUPPORT_BASE,os.path.join(hq_home,'support'))
    else:
        # If it's an agent installation
        if installation_type == AGENT:
            addGlobalData(gd.AGENT_HOME,hq_home)
            expected_jre_location = os.path.join(hq_home,"jre")
            if os.path.exists(expected_jre_location):
                addGlobalData(gd.AGENT_JRE,expected_jre_location)
                addGlobalData(gd.JRE,expected_jre_location)
            else:
                # add JAVA_HOME as the JRE path
                if 'JAVA_HOME' in os.environ.keys():
                    addGlobalData(gd.JRE,os.environ['JAVA_HOME'])
            addGlobalData(gd.SUPPORT_BASE,os.path.join(hq_home,'support'))
        else:
            # If it's a dev machine
            if installation_type == DEV:
                # Then add JAVA_HOME as the JRE path if it exists
                if 'JAVA_HOME' in os.environ.keys():
                    addGlobalData(gd.JRE,os.environ['JAVA_HOME'])
                addGlobalData(gd.SUPPORT_BASE,os.path.join(hq_home,'resources'))
                # Save the location of the support source-code project
                addGlobalData(gd.SUPPORT_PROJECT_BASE,os.path.join(hq_home,'..','..'))
            else:
                raise Exception("Unknown installation type")

    # Save lib folder as a variable
    lib_folder = os.path.join(gd.getGlobalData(gd.SUPPORT_BASE),'lib')
    addGlobalData(gd.LIB_FOLDER,lib_folder)

    # Save java executable location as a variable
    addGlobalData(gd.JAVA_EXECUTABLE,os.path.join(gd.getGlobalData(gd.JRE),'bin','java'))
    java_executable = gd.getGlobalData(gd.JAVA_EXECUTABLE)
    # Save jython JAR location as a variable
    jython_jar_location = os.path.join(gd.getGlobalData(gd.LIB_FOLDER),'jython.jar')
    addGlobalData(gd.JYTHON_JAR_LOCATION,jython_jar_location)
    # Save the command line to run jython as a variable
    run_jython = '%(java_executable)s -jar %(jython_jar_location)s ' % vars()
    addGlobalData(gd.RUN_JYTHON,run_jython)
    # Save the command line to run a platform-independent tar as a variable
    simple_tar_path = os.path.join(gd.getGlobalData(gd.SUPPORT_BASE),'scripts','common','simpletar.py')
    simple_tar_command = '%(run_jython)s %(simple_tar_path)s' % vars()
    addGlobalData(gd.SIMPLE_TAR,simple_tar_command)

    if installation_type == SERVER or installation_type == AGENT:
        # TODO Get sigar version automatically
        sigar_jar = os.path.abspath(os.path.join(gd.getGlobalData(gd.SUPPORT_BASE),'lib','sigar','sigar-1.6.6.jar'))
    else:
        # On dev machines, we need to take the sigar jar from the target folder
        sigar_jar = os.path.abspath(os.path.join(gd.getGlobalData(gd.SUPPORT_PROJECT_BASE),'target','generated-resources','lib-sigar','sigar-1.6.6.jar'))
    addGlobalData(gd.SIGAR_JAR,sigar_jar)

    addGlobalData(gd.RUN_SIGAR,'%(java_executable)s -jar %(sigar_jar)s' % vars())

