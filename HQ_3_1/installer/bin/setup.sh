#!/bin/sh

cd `dirname $0`
INSTALLBASE=`pwd`
${INSTALLBASE}/installer-@@@VERSION@@@/bin/hq-setup.sh $@
