#!/bin/bash

# Check for proper number of arguments
if [ $# -ne 3 ] ; then 
  echo "Error: Usage: $0 <rootdir> <release-number> <outfile>"
  exit 1
fi

# Get args, check for existence
ROOTDIR=${1}
if [ -z ${ROOTDIR} ] ; then 
  echo "Error: Usage: $0 <rootdir> <release-number> <outfile>"
  exit 1
fi
RELEASENUM=${2}
if [ -z ${RELEASENUM} ] ; then 
  echo "Error: Usage: $0 <rootdir> <release-number> <outfile>"
  exit 1
fi
OUTFILE=${3}
if [ -z ${OUTFILE} ] ; then 
  echo "Error: Usage: $0 <rootdir> <release-number> <outfile>"
  exit 1
fi

# Append to outfile
echo "" >> ${OUTFILE}
echo "# Tag Summary for ${RELEASENUM} :" >> ${OUTFILE}
echo "# " >> ${OUTFILE}
for cvsdir in `find ${ROOTDIR} -type d -name CVS` ; do
  if [ -a $cvsdir/Tag ] ; then
    echo "# `cat $cvsdir/Repository | tr '/' ' ' | awk '{print $1}'` :~~~`cat $cvsdir/Tag`"
  fi
done | uniq | sed 's/~~~T/ Branch /g' | sed 's/~~~N/ Tag /g' >> ${OUTFILE}

exit 0
