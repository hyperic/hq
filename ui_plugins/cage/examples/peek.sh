#!/bin/sh

curl http://hqadmin:hqadmin@localhost:7080/hqu/cage/cage/peek.hqu -Fargs=@"$1"
