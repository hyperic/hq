#!/bin/sh

curl http://hqadmin:hqadmin@localhost:7080/hqu/cage/cage/explain.hqu -dclass="$1"
