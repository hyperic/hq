#!/bin/sh

. etc/multiagent.properties

echo "Creating clone $1"

mkdir clones/clone_$1

for f in jaas.config lib pdk rcfiles
do
	ln -s ../master/$f clones/clone_$1/$f
done

ln -s ../wrapper clones/clone_$1/wrapper
cp hq-agent-nowrapper.sh  clones/clone_$1/hq-agent-nowrapper.sh

clone_port=$(($CLONE_LISTEN_STARTPORT + $1 + ( $2 * 200 )))

cat etc/agent.properties | \
	sed -e s/@CLONE_SERVERIP@/$CLONE_SERVERIP/       | \
	sed -e s/@CLONE_SECURE@/$CLONE_SECURE/           | \
	sed -e s/@CLONE_FQDN@/${CLONE_FQDN_PREFIX}$1/    | \
	sed -e s/@CLONE_SERVER_PORT@/$CLONE_SERVER_PORT/ | \
	sed -e s/@CLONE_LISTENPORT@/${clone_port}/ \
> clones/clone_$1/agent.properties

cd clones/clone_$1
nohup ./hq-agent-nowrapper.sh start 2>&1 > console.out & 
echo $! > nowrapper.pid
