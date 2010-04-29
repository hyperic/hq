#!/bin/sh

trap cleanup 0 1 2 3 6

cleanup()
{
  reset -IQ
  exit
}

cd `dirname $0`
INSTALLBASE=`pwd`
${INSTALLBASE}/installer/bin/hq-setup.sh $@
