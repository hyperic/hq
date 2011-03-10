#!/bin/sh
cd runtimeGroups
for dir in `ls`
do
cd $dir
pwd
./stop_multiagent.sh
cd ..
done
cd ..
