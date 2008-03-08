#!/bin/sh

PARMS=""
PARMS="${PARMS} "-dincludeSystem="false"
PARMS="${PARMS} "-dverbose="false"

curl http://hqadmin:hqadmin@localhost:7080/hqu/mass/group/list.hqu \
    ${PARMS}
