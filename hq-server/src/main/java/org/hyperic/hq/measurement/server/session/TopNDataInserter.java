/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.measurement.server.session;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.TopNManager;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TopNDataInserter implements DataInserter<TopNData> {

    private static final Log LOG = LogFactory.getLog(TopNDataInserter.class);
    private DataManager dataManager;
    private TopNManager topNManager;

    @Autowired
    public TopNDataInserter(DataManager dMan, TopNManager topNManager) {
        dataManager = dMan;
        this.topNManager = topNManager;
    }

    public void insertData(List<TopNData> topNData) {
        final boolean debug = LOG.isDebugEnabled();
        if (debug) LOG.debug(String.format("drained topn data queue with %d elements", topNData.size()));
        final StopWatch watch = new StopWatch();
        for (TopNData data : topNData) {
            byte[] uncompressed = data.getData();
            byte[] compressed = topNManager.compressData(uncompressed);
            data.setData(compressed);
        }
        dataManager.addTopData(topNData);
        if (debug) LOG.debug("Persisting " + topNData.size() + " topn data took: " + watch);
    }

    public Object getLock() {
        return new Object();
    }

    public void insertData(List<TopNData> metricData, boolean isPriority) throws InterruptedException, DataInserterException {
        insertData(metricData, false);
    }

    public void insertDataFromServer(List<TopNData> metricData) throws InterruptedException, DataInserterException {
        insertData(metricData, false);
    }
}
