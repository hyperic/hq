#!/bin/sh

thisdir=`dirname $0`
cd ${thisdir}
cd ..
HQ_DIR=`pwd`
cd hqdb
HQDB_DIR=`pwd`
ARCHIVE_DIR=${HQ_DIR}/archive
TS=`date +%m.%d.%y`
OUTFILE=${ARCHIVE_DIR}/hqdb-${TS}.dump.gz

if [ ! -d "${ARCHIVE_DIR}" ]; then
    echo "Creating archive directory ${ARCHIVE_DIR}"
    mkdir ${ARCHIVE_DIR}
fi

# Check existing dump?
echo "Archving database to ${OUTFILE}"

LD_LIBRARY_PATH=${HQDB_DIR}/lib \
DYLD_LIBRARY_PATH=${HQDB_DIR}/lib \
SHLIB_PATH=${HQDB_DIR}/lib \
  ${HQDB_DIR}/bin/pg_dump -h localhost -p @@@PGPORT@@@ -U hqadmin hqdb \
       | gzip > ${OUTFILE}

echo "Archive complete"
