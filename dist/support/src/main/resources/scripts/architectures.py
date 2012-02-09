#!/usr/bin/python

import os,sys
import logging
import global_data as gd

logger = logging.getLogger('architectures')

X86_64_LINUX = "x86-64-linux"
PPC_AIX = "ppc-aix"
SPARC_SOLARIS = "sparc-solaris"
WIN32 = "win32"
X86_LINUX = "x86-linux"
HPUX_11 = "hpux-11"
APPLE_OSX = "apple-osx"

architectures = {
                  X86_64_LINUX : "x86-64-linux" ,
                  PPC_AIX : "PPC_AIX" ,
                  SPARC_SOLARIS : "sparc-solaris" ,
                  WIN32 : "win32" ,
                  X86_LINUX : "x86-linux" ,
                  HPUX_11 : "hpux-11" ,
                  APPLE_OSX : "apple-osx" }

# Detect the system's architecture. Currently we only distinguish between 
# windows and all the others.
def detectArchitecture_internal():
    # TODO More elaborate way to detect all architectures. For now we just separate between
    # windows and linux types.
    try:
        # If we're in jython, then we can use the system property from java.
        # We currently just get it for debugging purposes but don't decide the architecture
        # based on it.
        from java.lang import System
        os_name = System.getProperty('os.name')
        logger.debug('os.name from java is %s' % os_name)
    except:
        pass
    # Check for OS environment variable
    if 'OS' in os.environ.keys() and ('Windows' in os.environ['OS']):
        logger.debug("Found OS environment variable containing 'Windows'. This ia windows machine")
        return architectures[WIN32]
    logger.debug('OS environment variable not found. Assuming this is a linux x86 machine.')
    return architectures[X86_LINUX]

def detectArchitecture():
    arch = detectArchitecture_internal()
    gd.addGlobalData(gd.ARCH,arch)
    return arch

