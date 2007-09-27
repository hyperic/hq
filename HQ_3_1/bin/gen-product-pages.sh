#!/bin/bash

WWWDIR=${1}
if [ "x${WWWDIR}" = "x" ] ; then
  echo "Usage: $0 <path-to-www.hyperic.com-repo> <output-dir>"
  exit 1
fi
OUTDIR=${2}
if [ "x${OUTDIR}" = "x" ] ; then
  echo "Usage: $0 <path-to-www.hyperic.com-repo> <output-dir>"
  exit 1
fi

JAVA=${JAVA_HOME}/bin/java
THISDIR=`pwd`
cd `dirname $0`/..
HQ_HOME=`pwd`
LIBDIR=${HQ_HOME}/thirdparty/lib

TEMPLATE=${WWWDIR}/registration/website/products/managed/management.htm

AGENT=${HQ_HOME}/build/agent

# Comma seperated list of plugins to exclude
EXCLUDE_PLUGINS=hqagent

CP=\
${LIBDIR}/velocity-1.3.1.jar:\
${LIBDIR}/ant.jar:\
${LIBDIR}/commons-beanutils.jar:\
${LIBDIR}/commons-logging.jar:\
${LIBDIR}/commons-collections.jar:\
${LIBDIR}/commons-httpclient-2.0.jar:\
${LIBDIR}/log4j.jar:\
${LIBDIR}/snmpmgr.jar:\
${LIBDIR}/jakarta-oro-2.0.7.jar:\
${LIBDIR}/jdom_b8.jar:\
${HQ_HOME}/hq_bin/sigar_bin/lib/sigar.jar:\
${HQ_HOME}/hq_bin/db2monitor_bin/lib/db2monitor.jar:\
${HQ_HOME}/hq_bin/citrixmonitor_bin/lib/citrix.jar:\
${HQ_HOME}/hq_bin/win32bindings_bin/lib/win32bindings.jar:\
${HQ_HOME}/build/lib/jxla.jar:\
${AGENT}/lib/AgentServer.jar:\
${AGENT}/pdk/lib/hyperic-util.jar:\
${AGENT}/pdk/lib/bcel-5.1.jar:\
${AGENT}/pdk/lib/hq-product.jar

cd ${THISDIR}

${JAVA} -Dpdk.dir=${AGENT}/pdk -Dplugins.exclude=${EXCLUDE_PLUGINS} -classpath ${CP} org.hyperic.hq.product.util.ProductPageDumper ${AGENT} ${TEMPLATE} ${OUTDIR}

chmod a+rx ${OUTDIR}/*.htm*
