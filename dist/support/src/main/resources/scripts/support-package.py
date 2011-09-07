#!/usr/bin/python

#
# Hyperic HQ 
# 
# This script is used to collect system and application information and create a support package.
# The support package can be sent to support/dev people for easier analysis of issues.
#
# The actual commands to run are defined in "data/support-commands.*". See data/support-commands.common for details.
#
# Internal commands used for the actual execution of this script are defined in data/helper-commands.*.
#
# Workflow is as follows:
#   * Detect installation type
#   * Create temporary folder ("base_working_folder") in which the resulting support package will be written (resulting compressed file)
#   * Create a subfolder of the temp folder ("detail_working_folder") in which all command output will be written. This folder will be the one that will eventuall 
#     be compressed into what we call a "support package"
#   * Run all commands that need to be executed according to the detected installation type
#   * Archive all the command output in one file
#   * Verify the archive file
#   * Remove the subfolder ("detail_working_folder"). "base_working_folder" is kept in tact and includes the final archive file.
#
# In case of an error during the workflow, the detail_working_folder is left in tact as well and not removed.

import os,sys,tempfile,glob,time,shutil
from subprocess import call
from hq_utils import *
from commands import *
import global_data as gd
from global_data import addGlobalData
from optparse import OptionParser
import logging
from utils import *
from architectures import *

# Initialize basic logging
logger = logging.getLogger('support-package')
add_console_logging_handler()
# Final logging initialization will be done only after we know the support package folder

# Create option parser
parser = OptionParser(usage="")
parser.add_option("-s", "--show-variable-list", dest="show_variable_list",action="store_true",
                  help="List all available variables")
parser.add_option("-n", "--dry-run", dest="dry_run",action="store_true",
                  help="Dry run - do not actually run the commands and the archiving - Note that temp folders are still created when using this option")
parser.add_option("-v", "--verbose", dest="verbose",action="store_true",
                  help="Verbose Mode")

# Parser arguments
options,args = parser.parse_args()

# Set logging according to verbosity
if options.verbose:
    logging.getLogger('').setLevel(logging.DEBUG)
else:
    logging.getLogger('').setLevel(logging.INFO)

# If requested, then just print all available variables and exit
if options.show_variable_list:
    gd.listGlobalDataRepository()
    sys.exit(0)
    
# Save current directory
original_current_dir = os.curdir
detail_folder_created = False

# NOTE: All addGlobalData calls create new variables which can be used by the command definitions.
#       Run "support-package.sh -l" for a complete list of all variables and their explanation.
#       See data/support-commands.common for detailed explanation.

keep_detail_folder = False
try:
    try:
        # Detect the architecture
        arch = detectArchitecture()
        addGlobalData(gd.ARCH,arch)
        # Find the path of the script
        scripts_path = os.path.split(os.path.abspath(sys.argv[0]))[0]
        addGlobalData(gd.SCRIPTS_PATH,os.path.abspath(scripts_path))
        # Detect HQ related information
        # NOTE: Assumption is that script is in $HQ_HOME/support/scripts/
        detectHQInformation(os.path.abspath(os.path.join(scripts_path,'..','..')))
        arch = gd.getGlobalData(gd.ARCH)

        # Save java executable location as a variable
        addGlobalData(gd.JAVA_EXECUTABLE,os.path.join(gd.getGlobalData(gd.JRE),'bin','java'))
        java_executable = gd.getGlobalData(gd.JAVA_EXECUTABLE)
        # Save lib folder as a variable
        lib_folder = os.path.join(scripts_path,'..','lib')
        addGlobalData(gd.LIB_FOLDER,lib_folder)
        # Save jython JAR location as a variable
        jython_jar_location = os.path.join(lib_folder,'jython.jar')
        addGlobalData(gd.JYTHON_JAR_LOCATION,jython_jar_location)
        # Save the command line to run jython as a variable
        run_jython = '%(java_executable)s -jar %(jython_jar_location)s ' % vars()
        addGlobalData(gd.RUN_JYTHON,run_jython)
        # Save the command line to run a platform-independent tar as a variable
        simple_tar_path = os.path.join(scripts_path,'common','simpletar.py')
        simple_tar_command = '%(run_jython)s %(simple_tar_path)s' % vars()
        addGlobalData(gd.SIMPLE_TAR,simple_tar_command)

        # Create base working folder and change to it - This is the folder that the resulting support 
        # package file will be written to.
        base_working_folder = tempfile.mkdtemp()
        logger.info("Creating a support package in folder %s" % base_working_folder)
        addGlobalData(gd.BASE_WORKING_FOLDER,base_working_folder)

        # Create the support package's name and full path
        support_package_name = 'hq-support-package-%10.0f' % time.time()
        addGlobalData(gd.SUPPORT_PACKAGE_NAME,support_package_name)
        support_package_full_path = os.path.join(base_working_folder,support_package_name)
        addGlobalData(gd.SUPPORT_PACKAGE_FULL_PATH,support_package_full_path)

        # Create the folder that actual command output will be written to (this is called the detail_working_folder)
        detail_working_folder = os.path.join(base_working_folder,support_package_name)
        addGlobalData(gd.DETAIL_WORKING_FOLDER,detail_working_folder)
        os.mkdir(detail_working_folder)
        # Now that we know the folder of the support package we can initialize logging (the log file is written
        # to the detail_working_folder so it becomes part of the support package). We save the logging file handler
        # So we'll be able to close it properly afterwards
        logging_file_handler = add_logging_handler(detail_working_folder,"support-package-%s.log" % support_package_name)
        detail_folder_created = True

        installation_type = gd.getGlobalData(gd.INSTALLATION_TYPE)
        logger.info("Installation type is %s" % installation_type)
        # Assumption is that data folder is a peer of the scripts folder (e.g. support/data and support/scripts)
        # Read all support commands
        support_commands = CommandList(os.path.join(scripts_path,'..','data','support-commands'),arch,installation_type)
        # Read helper commands (e.g. for archiving and verifying the archive
        helper_commands = CommandList(os.path.join(scripts_path,'..','data','helper-commands'),arch,installation_type)

        # Execute all commands
        if not options.dry_run:
            support_commands.executeAll(detail_working_folder,gd.globalData)
    except Exception, err:
        logger.exception('Unhandled Exception. Trying to archive anyway')
    
    if not options.dry_run:
        try:
            # Get the archive command from the helper commands repository
            archive_command = helper_commands.getCommand('archive')
            # Get the verify-archive command from the helper comamnds repository
            verify_archive_command = helper_commands.getCommand('verify-archive')
            if archive_command is None:
                logger.error("Cannot find archive command in helper commands. Failing and keeping detail folder in tact")
                keep_detail_folder = True
            else:
                # Archive all the files
                archive_command.execute(detail_working_folder,gd.globalData)

                # Verify the archive
                retcode = verify_archive_command.execute(detail_working_folder,gd.globalData) 
                # If verification failed, 
                if retcode != 0:
                    # then keep detail folder
                    logger.error('Archive test failed! Keeping detail folder in tact')
                    keep_detail_folder = True
                else:
                    logger.info("Resulting support package is in %s . Archive filename is %s (extension is platform dependent)" % (base_working_folder,support_package_name))
        except Exception, err:
            logger.exception('Could not archive support package. Keeping detail folder')
            keep_detail_folder = True
finally:
    if not options.dry_run:
        # If detail folder has been created
        if detail_folder_created:
            # If we don't need to keep it,
            if not keep_detail_folder:
                try:
                    # Close the log file so we can remove the folder
                    logging_file_handler.close()
                    # then try to remove it
                    shutil.rmtree(detail_working_folder)
                except Exception,err:
                    logger.exception("Could not remove detail working folder (%s)" % detail_working_folder)
            else:
                # Keep it
                logger.info("Detail folder has not been deleted - The folder is %s" % detail_working_folder)
        # Revert to original working folder
        os.chdir(original_current_dir)

