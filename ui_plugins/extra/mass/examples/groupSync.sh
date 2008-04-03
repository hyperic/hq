#!/bin/sh

ARGS="`cat $1`"
curl http://hqadmin:hqadmin@localhost:7080/hqu/mass/group/sync.hqu -dargs="$ARGS"
