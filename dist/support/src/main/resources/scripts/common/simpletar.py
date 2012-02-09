#!/usr/bin/python

# A simple version for tarring files in case there is no way to compress files in the target OS.

import sys,os, fnmatch
import glob
from optparse import OptionParser
import tarfile
import logging

logger = logging.getLogger("simpletar")

if len(logger.handlers) == 0:
    logger.addHandler(logging.StreamHandler())

def recursive_glob(pattern):
    results = []
    (folder,fp) = os.path.split(pattern)
    if fp == '':
        for base, dirs, files in os.walk(folder):
            results.extend([os.path.join(base, f) for f in files])
        return results
    else:
        if os.path.isdir(pattern):
            lst = glob.glob(os.path.join(pattern,'*'))
        else:
            lst = glob.glob(pattern)
        folderList = []
        filesList = []
        for f in lst:
            if os.path.isdir(f):
                folderList.append(f)
            else:
                filesList.append(f)
        for d in folderList:
            results.extend(recursive_glob(d))
        results.extend(filesList)
        return results
  
  

usage = """
    Simplistic tar aggregation - Accepts only a folder to pack

    simpletar.py [options] <target-tar-file> <file-pattern1> ...

    NOTE: This is not a complete tar implementation, even not to the point of accepting multiple filenames in the 
          command line.
"""

parser = OptionParser(usage=usage)
parser.add_option("-v", "--verbose", dest="verbose",action="store_true",
                  help="Verbose Mode")

options,args = parser.parse_args()

if len(args) < 2:
    parser.usage()
    sys.exit(1)

targetTarFile = args[0]
filePatterns = args[1:]

if options.verbose:
    logger.setLevel(logging.DEBUG)
else:
    logger.setLevel(logging.INFO)

tar = None
try:
    try:
        effectiveFileList = []
        for pattern in filePatterns:
            effectiveFileList.extend(recursive_glob(pattern))

        tar = tarfile.open(targetTarFile,"w:gz")
        for f in effectiveFileList:
            tar.add(f)
            if options.verbose:
                logger.info("Adding %s" % f)
    except Exception,e:
        logger.exception("Failed to create tar file")
        sys.exit(10)
finally:
    if tar is not None:
        tar.close()
        logger.debug("%s has been created" % targetTarFile)

