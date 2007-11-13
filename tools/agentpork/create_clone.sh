#!/bin/sh

. agentpork.cfg

echo "Creating clone $1"

mkdir clones/clone_$1

for f in jaas.config lib pdk rcfiles
do
	ln -s ../master/$f clones/clone_$1/$f
done

ln clones/master/hq-agent.sh clones/clone_$1/hq-agent.sh

clone_port=$(($CLONE_LISTEN_STARTPORT + $1))

cat etc/agent.properties | \
	sed -e s/@CLONE_SERVERIP@/$CLONE_SERVERIP/       | \
	sed -e s/@CLONE_SECURE@/$CLONE_SECURE/           | \
	sed -e s/@CLONE_FQDN@/${CLONE_FQDN_PREFIX}$1/    | \
	sed -e s/@CLONE_LISTENPORT@/${clone_port}/ \
> clones/clone_$1/agent.properties

(cd clones/clone_$1 && ./hq-agent.sh start && ./hq-agent.sh stop)
