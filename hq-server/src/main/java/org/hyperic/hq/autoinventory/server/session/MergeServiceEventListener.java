/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
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

package org.hyperic.hq.autoinventory.server.session;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.autoinventory.server.session.RuntimeReportProcessor.ServiceMergeInfo;
import org.hyperic.hq.zevents.Zevent;
import org.hyperic.hq.zevents.ZeventEnqueuer;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.util.CollectionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
@Component
public class MergeServiceEventListener implements ZeventListener<MergeServiceReportZevent> {

    // Number of services which we attempt to merge in a single tx
    private static final int BATCH_SIZE = 1000;

    private ZeventEnqueuer zeventManager;

    private ServiceMerger serviceMerger;

    private final Log log = LogFactory.getLog(MergeServiceEventListener.class);
    
    
    @Autowired
    public MergeServiceEventListener(ZeventEnqueuer zeventManager, ServiceMerger serviceMerger) {
        this.zeventManager = zeventManager;
        this.serviceMerger = serviceMerger;
    }

    @PostConstruct
    public void subscribeForEvents() {
        Set<Class<? extends Zevent>> events = new HashSet<Class<? extends Zevent>>();
        events.add(MergeServiceReportZevent.class);
        zeventManager.addBufferedListener(events, this);

    }

    public void processEvents(List<MergeServiceReportZevent> events) {
        final List<ServiceMergeInfo> sInfos = new ArrayList<ServiceMergeInfo>(BATCH_SIZE);
        final int size = events.size();
        for (int i=0; i<size; i+=BATCH_SIZE) {
            int end = Math.min(size, i+BATCH_SIZE);
            final List<MergeServiceReportZevent> batch = events.subList(i, end);

            try {
                sInfos.clear();
                for (MergeServiceReportZevent zv : batch ) {
                    ServiceMergeInfo sInfo = zv.getMergeInfo();
                    sInfos.add(sInfo);
                }
                serviceMerger.mergeServices(sInfos);
            } catch (Exception e) {
                log.warn("Error merging services", e);
            }
        }
    }
}
