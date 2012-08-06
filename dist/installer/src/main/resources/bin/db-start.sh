#!/bin/sh

thisdir=`dirname $0`
cd ${thisdir}
cd ..
HQ_DIR=`pwd`
cd hqdb
HQDB_DIR=`pwd`

if [ "`uname -s`" = "HP-UX" ]; then
  echo "Enabling special attributes for HP-UX"
  chatr +s enable ${HQDB_DIR}/bin/* > /dev/null 2>&1
  chatr +s enable ${HQDB_DIR}/lib/*.sl > /dev/null 2>&1
fi

LD_LIBRARY_PATH=${HQDB_DIR}/lib \
DYLD_LIBRARY_PATH=${HQDB_DIR}/lib \
SHLIB_PATH=${HQDB_DIR}/lib \
  ${HQDB_DIR}/bin/pg_ctl  \
    -D ${HQDB_DIR}/data   \
    -l ${HQ_DIR}/logs/hqdb.log start
   

