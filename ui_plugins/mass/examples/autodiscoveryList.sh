#!/bin/sh
#
# List platforms currently in the auto-discovery queue.
# 
# This script takes 1 optional fqdn argument.  This can be either an exact fqdn or a regex.
#
# Examples:
#
# List the platform in the queue with the FQDN www.hyperic.com
# ./autoinventoryList.sh -dfqdn="www.hyperic.com"
#
# List all platforms
# ./autoinventoryList.sh -dfqdn="regex:.*"
#
# List all platforms that end with the domain hyperic.com
# ./autoinventoryList.sh -dfqdn="regex:.*hyperic.com" # Approve all platforms that match *.hyperic.com
#

curl -u hqadmin http://localhost:7080/hqu/mass/autodiscovery/list.hqu $@
