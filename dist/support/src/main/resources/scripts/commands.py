#!/usr/bin/python

#
# This module handles command definitions and lists of command definitions.
#
# It handles two levels of command defintion files - One common, and one specific for the system's architecture

import os,sys
from subprocess import call
import logging

logger = logging.getLogger('commands')

# Class for holding one command definition
class Command(object):
    # command_string is one line in the format " <name> : <installation_types> : <runtime-folder> : <command> "
    # (Usually read from a file)
    def __init__(self,command_string):
        # Split the command string
        s = command_string.split(":",3)
        if len(s) != 4:
            logger.error("Command is not in the correct format (%s)" % command_string)
            raise Exception("Command is not in the correct format (%s)" % command_string)

        # Save the command definition fields
        self.name = s[0].strip()
        self.installation_types = s[1].strip()
        self.cmd_folder = s[2].strip()
	self.cmd = s[3].strip()

        self.last_retcode = None

    # Execute the actual command
    def execute(self,working_folder,globalParams={},localParams={}):
        logger.info("Executing %s" % self.name)
        # Save current folder
        saved_cwd = os.curdir
        try:
            # Merge global and local parameters to form one variable-set
            all_params = {}
            all_params.update(globalParams)
            all_params.update(localParams)
            try:
                # Interpolate the variable set to get the actual command (template -> actual string)
                actual_cmd = self.cmd % all_params
            except:
                logger.error("Failed to create actual command for command '%s' - The command text probably contains a non-existent variable (command string is '%s'). Skipping" % (self.name,self.cmd))
                return
            # If the runtime folder is not '.',
            if self.cmd_folder != '.':
                # Then move to the runtime folder
                new_cwd = self.cmd_folder % all_params
                os.chdir(new_cwd)
                logger.debug("Changed folder to %s" % new_cwd)
            logger.debug("Command line to execute is '%s'" % actual_cmd)
            sout = file(os.path.join(working_folder,self.name+".output"),"w")
            serr = file(os.path.join(working_folder,self.name+".error"),"w")
            try:
                # Execute the command
                retcode = call(actual_cmd,stdout=sout,stderr=serr,shell=True)
            finally:
                sout.close()
                serr.close()
            # Save last return code
            self.last_retcode = retcode
            # Return the return code
            return retcode
        finally:
            # Go back to the original folder we've been in before the command execution
            os.chdir(saved_cwd)

    def __eq__(self,other):
        return self.name == other.name
    def __hash__(self):
        return self.name.__hash__()
    def __cmp__(self,other):
        return self.name.__cmp__(other.name)

    def __str__(self):
        return "<name=%s,cmd=%s>" % (self.name,self.cmd)
    __repr__ = __str__

# Maintain a set of command definition, spread over two files - a .common file and .<arch> file
class CommandList(object):
    # Initialization:
    #   * filename_prefix - Prefix of the two files
    #   * arch - The architecture of the system
    #   * installation_type - The actual installation type detected
    def __init__(self,filename_prefix,arch,installation_type):
        self.installation_type = installation_type
        self.loadCommandDefinitions(filename_prefix+".common",filename_prefix+".%s" % arch)

    def loadCommandDefinitions(self,common_filename,arch_filename):
        # Initialize map for holding command definitions
        command_dict = {}
        # Load definitions from .common file 
        self.loadDefinitionsFromFile(command_dict,common_filename)
        # Load definitions from .<arch> file
        self.loadDefinitionsFromFile(command_dict,arch_filename)
        self.all_commands = command_dict.values()
        logger.debug("Effective commands to run: %s" % [x.name for x in self.all_commands])

    def loadDefinitionsFromFile(self,command_dict,filename):
        # If the file exists,
        if os.path.exists(filename):
            logger.debug("Loading definitions from %s" % filename)
            # Run through its lines
            for line in file(filename).read().splitlines():
                line = line.strip()
                # If it's a comment or an empty line, then just skip it
                if len(line) == 0 or line[0] == '#':
                    continue
                cmd = Command(line)
                # If installation_type is none,
                if cmd.installation_types == 'none':
                    logger.debug("Installation type is none")
                    # And it's a wildcard,
                    if cmd.name == '*':
                        logger.debug("Command name is a wildcard, so removing all previous command definitions")
                        # Then it's a special mark for clearing the previous command definitions
                        command_dict.clear()
                    else:
                        # Otherwise, it's a mark to remove any existing definition with the same name, if there is one
                        if cmd.name in command_dict.keys():
                            logger.debug("Deleting previous command definition for %s" % cmd.name)
                            del command_dict[cmd.name]
                            continue
                        else:
                            logger.warning("Tried to remove a non-existing command definition (%s)" % cmd.name)
                else:
                    # Check if wildcard is misused
                    if cmd.name == '*':
                        logger.error('Wildcard command name can only be used to remove previous definitions by using "none" as the installation type (installation_type is %s))' % cmd.installation_types)
                        continue
                    # Handle the command only if it's meant for a matching installation type
                    if self.isMatchingInstallationType(cmd):
                        if not cmd.name in command_dict.keys():
                            logger.debug("Command '%s' matches current installation type. Adding it" % cmd.name)
                        else:
                            logger.debug("Command '%s' matches current installation type and definition already exists. Overriding it" % cmd.name)
                        command_dict[cmd.name] = cmd
                    else:
                        logger.debug("Command '%s' does not match the current installation type. Skipping (current=%s , actual=%s)" % (cmd.name,self.installation_type,cmd.installation_types))
                
    # Check if a command's installation_types matches the actual installation type on the system
    def isMatchingInstallationType(self,command):
        installation_types = command.installation_types.split(",")
        return ('all' in installation_types) or (self.installation_type in installation_types)

    # Execute all the commands
    #   working_folder - The folder to write the output to
    #   globalParams - Variables to use when needed
    def executeAll(self,working_folder,globalParams={}):
        # Go over all the commands,
        for cmd in self.all_commands:
            # try each command and skip if failed
            try:
                retcode = cmd.execute(working_folder,globalParams)
                if retcode != 0:
                    logger.warning("Command '%s' returned failure code (%d)" % (cmd.name,retcode))
            except Exception,err:
                logger.exception('Command (%s) execution threw an exception. Continuing to next command' % cmd.name)

    def getCommand(self,name):
        l = [x for x in self.all_commands if x.name == name]
        if len(l) > 0:
            return l[0]
        else:
            return None

