#!/bin/sh
#
# Approve platforms currently in the auto-discovery queue.
#
# This script takes 1 optional fqdn argument.  This can be either an exact fqdn or a regex.
#
# Examples:
#
# Approve a single platform with the FQDN www.hyperic.com
# ./autoinventoryApprove.sh -dfqdn="www.hyperic.com"
#
# Approve all platforms
# ./autoinventoryApprove.sh -dfqdn="regex:.*"
#
# Approve all platforms that end with the domain hyperic.com
# ./autoinventoryApprove.sh -dfqdn="regex:.*hyperic.com" # Approve all platforms that match *.hyperic.com
#

curl -uhqadmin http://localhost:7080/hqu/mass/autodiscovery/approve.hqu $@
