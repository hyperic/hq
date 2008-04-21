#!/bin/sh

curl http://hqadmin:hqadmin@localhost:7080/hqu/cage/cage/sync.hqu -Fargs=@"$1"
