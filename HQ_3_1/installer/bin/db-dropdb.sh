#!/bin/sh

thisdir=`dirname $0`
cd ${thisdir}
cd ../hqdb
HQDB_DIR=`pwd`

LD_LIBRARY_PATH=${HQDB_DIR}/lib \
DYLD_LIBRARY_PATH=${HQDB_DIR}/lib \
SHLIB_PATH=${HQDB_DIR}/lib \
  ${HQDB_DIR}/bin/dropdb -U hqadmin -p @@@PGPORT@@@ $@
