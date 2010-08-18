#!/bin/sh
#
# NOTE: This copyright does *not* cover user programs that use HQ
# program services by normal system calls through the application
# program interfaces provided as part of the Hyperic Plug-in Development
# Kit or the Hyperic Client Development Kit - this is merely considered
# normal use of the program, and does *not* fall under the heading of
#  "derived work".
#
#  Copyright (C) [2009-2010], VMware, Inc.
#  This file is part of HQ.
#
#  HQ is free software; you can redistribute it and/or modify
#  it under the terms version 2 of the GNU General Public License as
#  published by the Free Software Foundation. This program is distributed
#  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
#  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
#  PARTICULAR PURPOSE. See the GNU General Public License for more
#  details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program; if not, write to the Free Software
#  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
#  USA.
#
#

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
