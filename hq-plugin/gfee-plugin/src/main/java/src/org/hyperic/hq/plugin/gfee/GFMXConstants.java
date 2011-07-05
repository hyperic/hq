/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.plugin.gfee;

public class GFMXConstants {
	
    // jmx domains
    public final static String DOMAIN_GEMFIRE           = "GemFire";
    public final static String DOMAIN_GEMFIRECACHE      = "GemFire.Cache";
    public final static String DOMAIN_GEMFIRECACHEVM    = "GemFire.CacheVm";
    public final static String DOMAIN_GEMFIREMEMBER     = "GemFire.Member";
    public final static String DOMAIN_GEMFIRESTATISTICS = "GemFire.Statistic";
    public final static String DOMAIN_APPLICATION 		= "GemFire.Member";

    // GemFireAgent MBean
    public final static String AGENT_OBJ_NAME = DOMAIN_GEMFIRE + ":type=Agent";
    public final static String AGENT_OP_CONNECTTOSYSTEM = "connectToSystem";

    // AdminDistributedSystem MBean
    public final static String ADS_OBJ_NAME = DOMAIN_GEMFIRE + ":type=AdminDistributedSystem";
    public final static String ADS_OP_MANAGECACHESERVERS = "manageCacheServers";
    public final static String ADS_OP_MANAGESYSTEMMEMBERAPPLICATIONS = "manageSystemMemberApplications";

    // SystemMember MBean
    public final static String CACHEVM_OBJ_NAME = DOMAIN_GEMFIRECACHEVM + ":type=CacheVm";
    public final static String CACHEVM_OP_MANAGESTATS = "manageStats";
    public final static String CACHEVM_OP_MANAGECACHE = "manageCache";

    // Application MBean
    public final static String APPLICATION_OBJ_NAME = DOMAIN_APPLICATION + ":type=Application";	
    public final static String APPLICATION_OP_MANAGESTATS = "manageStats";

    // Statistics Resource MBean
    public final static String STATS_OBJ_NAME                       = DOMAIN_GEMFIRESTATISTICS + ":";
    public final static String STATS_OP_REFRESH                     = "refresh";
    public final static String STATS_NAME_VMSTATS                   = "vmStats";
    public final static String STATS_NAME_VMHEAPMEMORYSTATS         = "vmHeapMemoryStats";
    public final static String STATS_NAME_VMNONHEAPMEMORYSTATS      = "vmNonHeapMemoryStats";
    public final static String STATS_NAME_CODECACHENONHEAPMEMORY    = "Code Cache-Non-heap memory";
    public final static String STATS_NAME_EDENSPACEHEAPMEMORY       = "PS Eden Space-Heap memory";
    public final static String STATS_NAME_SURVIRORSPACEHEAPMEMORY   = "PS Survivor Space-Heap memory";
    public final static String STATS_NAME_OLDGENHEAPMEMORY          = "PS Old Gen-Heap memory";
    public final static String STATS_NAME_PERMGENNONHEAPMEMORY      = "PS Perm Gen-Non-heap memory";
    public final static String STATS_NAME_SCAVENGE                  = "PS Scavenge";
    public final static String STATS_NAME_MARKSWEEP                 = "PS MarkSweep";

    // Role bitmask values
    public final static int MEMBER_ROLE_GATEWAYHUB      = 1;
    public final static int MEMBER_ROLE_CACHESERVER     = 2;
    public final static int MEMBER_ROLE_APPLICATIONPEER = 4;

    // Config constants
    public final static String CONF_JMX_URL         = "jmx.url";
    public final static String CONF_JMX_USERNAME    = "jmx.username";
    public final static String CONF_JMX_PASSWORD    = "jmx.password";
    public final static String CONF_STATNAME        = "statname";
    
    public final static String CONF_VMSTATS                 = "vmstats";
    public final static String CONF_VMHEAPMEMORYSTATS       = "vmheapmemorystats";
    public final static String CONF_VMNONHEAPMEMORYSTATS    = "vmnonheapmemorystats";
    public final static String CONF_CODECACHE               = "codecache";
    public final static String CONF_YOUNGEDEN               = "youngeden";
    public final static String CONF_YOUNGSURVIVOR           = "youngsurvivor";
    public final static String CONF_OLDTENURED              = "oldtenured";
    public final static String CONF_OLDPERMANENT            = "oldpermanent";
    public final static String CONF_GCCHEAP                 = "gccheap";
    public final static String CONF_GCEXPENSIVE             = "gcexpensive";

    // Random constants
    public final static String ATTR_PWD             = "workingDirectory";
    public final static String ATTR_HOST            = "host";
    public final static String ATTR_NAME            = "name";
    

}
