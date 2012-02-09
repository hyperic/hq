#!/usr/bin/python

#
# Frontend for running support related commands on hyperic installations.
#
# Opens as an interactive shell if no parameters are passed, otherwise runs
# the command and parameters that have been passed on the command line.
#
# Enter help to see available commands.
#
# 
# Adding a new command is done by just adding a do_<command>(self,line) method to the 
# SupportCmd class. The following will apply:
# * line will contain a string with all the command parameters.
# * The method's documentation becomes the command's help (Python documents methods by 
#   using a """text....""" comment as the first line *inside* the method).
# * If you want a more complicated help (e.g. dynamic), then just implement a help_<command>(self,line)
#   method which prints the help text.
#
# No need to reinvent the wheel - Use run_with_jython and run_java_jar functions in order to perform
# the things that you need.
#
# Global variables can be accessed using gd.getGlobalVariable.


import cmd
import subprocess
import os,sys

import global_data as gd
import hq_utils
from architectures import *

def run_with_jython(script_name,arg_str):
    run_jython = gd.getGlobalData(gd.RUN_JYTHON)
    s = "%s %s %s" % (run_jython,script_name,arg_str)
    subprocess.call(s,shell=True)

def run_java_jar(jar_path,arg_str):
    java_executable = gd.getGlobalData(gd.JAVA_EXECUTABLE)
    s = "%s -jar %s %s" % (java_executable,jar_path,arg_str)
    subprocess.call(s,shell=True)

class SupportCmd(cmd.Cmd):

    DELEGATED_HELP_COMMANDS = ['package']

    def __init__(self,base_folder):
        self.base_folder = base_folder
        cmd.Cmd.__init__(self)

    def get_cmd_script_name(self,cmd_name):
        return os.path.join(self.base_folder,'support-%s.py' % cmd_name)

    def do_package(self,line):
        run_with_jython(self.get_cmd_script_name('package'),line)

    # Override the help command in order to delegate the help printing to the script
    # If it is the one implementing the command
    def do_help(self,arg):
        if arg in SupportCmd.DELEGATED_HELP_COMMANDS:
            run_with_jython(self.get_cmd_script_name(arg),'--help')
        else:
            cmd.Cmd.do_help(self,arg) 

    def do_get_variables(self,line):
        """get_variables - Retrieves the values of all support-related variables"""
        for k in sorted(gd.globalData.keys()):
            print "%s: %s" % (k,gd.globalData[k])
        
    def do_sigar(self,line):
        """Run a sigar command. See help for details"""
        if line.strip() != '':
            sigar_jar = gd.getGlobalData(gd.SIGAR_JAR)
            if not os.path.isfile(sigar_jar):
                if gd.getGlobalData(gd.INSTALLATION_TYPE) == hq_utils.DEV:
                    print "Could not find sigar JAR file - Please build the project before using this command"
                else:
                    print "Could not find sigar JAR file in. Expected location is %s"  % sigar_jar
                return
            run_java_jar(sigar_jar,line)
        else:
            print "sigar command parameters are missing. Use 'sigar help' for details"

    def help_sigar(self):
        print "Run a sigar command\n"
        sigar_jar = gd.getGlobalData(gd.SIGAR_JAR)
        run_java_jar(sigar_jar,'help')
        
    def do_EOF(self,line):
        return True

    def do_quit(self,line):
        """quit - Quit the frontend"""
        return True

    def postloop(self):
        print

    def emptyline(self):
        pass


detectArchitecture()
# Find the path of the script
scripts_path = os.path.split(os.path.abspath(sys.argv[0]))[0]
# NOTE: Assumption is that script is in $HQ_HOME/support/scripts/
hq_utils.detectHQInformation(os.path.abspath(os.path.join(scripts_path,'..','..')))

s = SupportCmd(scripts_path)
s.prompt = "hq>"

try:
    if len(sys.argv) > 1:
        s.onecmd(" ".join(sys.argv[1:]))
    else:
        print "Hyperic HQ Support Frontend. Enter help to see available commands."
        print "HQ installation type is %s" % gd.getGlobalData(gd.INSTALLATION_TYPE)
        print "JRE folder %s" % os.path.abspath(gd.getGlobalData(gd.JRE))
        print "Jython JAR location: %s" % os.path.abspath(gd.getGlobalData(gd.JYTHON_JAR_LOCATION))
        s.cmdloop()
except KeyboardInterrupt,e:
    pass
