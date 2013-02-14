/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.plugin.sharepoint;

import java.util.*;

public class SharePointServerDetector2010 extends SharePointServerDetectorDefault {
    /**
     * group , instance , counter
     */
    private final static List<String[]> statsServicesNameList = Arrays.asList(new String[][]{
                {"Records Management Counters", null, "Search results processed / sec base"},
                {"Publishing Cache", "_total", "Total object discards"},
                {"Disk-Based Cache", null, "Old Blob Cache Folders Pending Delete"},
                {"Foundation Search Gatherer Projects", "_total", "Gatherer Master Flag"},
                {"Foundation Search Schema Plugin", "_total", "Total Documents"},
                {"Foundation BDC Online", null, "Total calls failed"},
                {"Foundation Search Gatherer", null, "Filter Processes Terminated 02"},
                {"Foundation Search Indexer Plugin", "_total", "Persistent Indexes Propagated"},
                {"Foundation Search Query Processor", null, "Security Descriptor Cache Misses"},
                {"Foundation Search FAST Content Plugin", null, "Batches Failed Timeout"},
                {"Foundation Search Archival Plugin", "_total", "Queues Committing"},
                {"Foundation BDC Metadata", null, "Cache misses per second"},
                {"Foundation Search Gatherer Databases", "_total", "Documents in the crawl history"}
            });


    protected String servicesPrefix() {
        return "SharePoint ";
    }

    protected List<String[]> getStatsServicesNameList() {
        return statsServicesNameList;
    }
}
