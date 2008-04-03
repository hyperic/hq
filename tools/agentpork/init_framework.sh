#!/bin/sh

mkdir clones
cd clones
ln -s ../../../build/agent master
ln -s master/lib lib
ln -s master/pdk pdk

cd ..
ln -s clones/pdk pdk
ln -s clones/lib lib
