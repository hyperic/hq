/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2012], VMWare, Inc.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.shared.AgentManager;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.MeasurementProcessor;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly=true)
public class SrnManagerImpl implements SRNManager {
    
    private final Log log = LogFactory.getLog(SrnManagerImpl.class);
    @Autowired
    private MeasurementProcessor measurementProcessor;
    @Autowired
    private AgentScheduleSynchronizer agentScheduleSynchronizer;
    @Autowired
    private AgentManager agentManager;
    @Autowired
    private ZeventManager zeventManager;
    @Autowired
    private ScheduleRevNumDAO scheduleRevNumDAO;
    @Autowired
    private MeasurementManager measurementManager;
    @Autowired
    private ResourceManager resourceManager;
    /** 
     * if restrictions are not overridden a resource can only schedule once per this
     * time period.  {@link Boolean} overrideRestictions overrides this constraint.
     */
    private static final long SCHEDULE_RESTRICTION = MeasurementConstants.HOUR;

    public SrnManagerImpl() {}

    public ScheduleRevNum get(AppdefEntityID aeid) {
        return scheduleRevNumDAO.get(new SrnId(aeid));
    }

    @Transactional(readOnly=false)
    public void removeSrn(AppdefEntityID aeid) {
        if (aeid == null) {
            return;
        }
        final ScheduleRevNum srn = get(aeid);
        if (srn == null) {
            return;
        }
        scheduleRevNumDAO.remove(srn);
    }

    @Transactional(readOnly=false)
    public int incrementSrn(AppdefEntityID aeid) {
        return incrementSrn(aeid, now());
    }

    private int incrementSrn(AppdefEntityID aeid, long now) {
        final ScheduleRevNum srn = get(aeid);
        if (srn == null) {
            return scheduleRevNumDAO.create(aeid).getSrn();
        } else {
            final int newSrn = srn.getSrn() + 1;
            srn.setSrn(newSrn);
            srn.setLastSchedule(now);
            return newSrn;
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }

    @Transactional(readOnly=true)
    public void scheduleInBackground(Collection<AppdefEntityID> aeids, boolean overrideRestrictions) {
        reschedule(aeids, false, true, overrideRestrictions);
    }

    @Transactional(readOnly=false)
    public void scheduleInBackground(Collection<AppdefEntityID> aeids, boolean incrementSrns,
                                     boolean overrideRestrictions) {
        reschedule(aeids, incrementSrns, true, overrideRestrictions);
    }

    @Transactional(readOnly=false)
    public void schedule(Collection<AppdefEntityID> aeids, boolean incrementSrns, boolean overrideRestrictions) {
        reschedule(aeids, incrementSrns, false, overrideRestrictions);
    }

    private void reschedule(Collection<AppdefEntityID> aeids, boolean incrementSrns,
                            boolean scheduleInBackground, boolean overrideRestrictions) {
        if (aeids == null || aeids.isEmpty()) {
            return;
        }
        aeids = new HashSet<AppdefEntityID>(aeids);
        final Set<AppdefEntityID> toReschedule = new HashSet<AppdefEntityID>();
        final Set<AppdefEntityID> toUnschedule = new HashSet<AppdefEntityID>();
        setScheduleObjs(aeids, toReschedule, toUnschedule, overrideRestrictions);
        incrementSrns(incrementSrns, toReschedule);
        scheduleInBackground(scheduleInBackground, toReschedule, toUnschedule);
        scheduleInForeground(scheduleInBackground, toReschedule, toUnschedule);
    }

    private void scheduleInForeground(boolean scheduleInBackground, Set<AppdefEntityID> toReschedule,
                                      Set<AppdefEntityID> toUnschedule) {
        if (scheduleInBackground) {
            return;
        }
        if (!toReschedule.isEmpty()) {
            measurementProcessor.scheduleSynchronous(toReschedule);
        }
        if (!toUnschedule.isEmpty()) {
            measurementProcessor.unschedule(toUnschedule);
        }
    }

    private void scheduleInBackground(boolean scheduleInBackground, Set<AppdefEntityID> toReschedule,
                                      Set<AppdefEntityID> toUnschedule) {
        if (!scheduleInBackground) {
            return;
        }
        enqueueRescheduleEventAfterCommit(toReschedule);
        if (!toUnschedule.isEmpty()) {
            final Map<Integer, Collection<AppdefEntityID>> agentMap =
                agentManager.getAgentMap(toUnschedule);
            for (final Entry<Integer, Collection<AppdefEntityID>> entry : agentMap.entrySet()) {
                final Integer agentId = entry.getKey();
                final Agent agent = agentManager.getAgent(agentId);
                final Collection<AppdefEntityID> list = entry.getValue();
                final AgentUnscheduleZevent zevent = new AgentUnscheduleZevent(list, agent.getAgentToken());
                zeventManager.enqueueEventAfterCommit(zevent);
            }
        }
    }

    private void incrementSrns(boolean incrementSrns, final Set<AppdefEntityID> toIncrement) {
        if (incrementSrns) {
            incrementSrns(toIncrement);
        }
    }

    private void setScheduleObjs(Collection<AppdefEntityID> aeidList, Set<AppdefEntityID> toReschedule,
                                 Set<AppdefEntityID> toUnschedule, boolean overrideRestrictions) {
        final long now = now();
        final Collection<AppdefEntityID> aeids = new ArrayList<AppdefEntityID>(aeidList);
        for (final Iterator<AppdefEntityID> it=aeids.iterator(); it.hasNext(); ) {
            final AppdefEntityID aeid = it.next();
            final ScheduleRevNum srn = scheduleRevNumDAO.get(new SrnId(aeid));
            // if srn hasn't been created then we should either schedule or unschedule, don't skip
            if (srn != null && !canSchedule(srn, now, overrideRestrictions)) {
                it.remove();
                continue;
            }
        }
        final Map<Integer, List<Measurement>> enabled = measurementManager.findEnabledMeasurements(aeids);
        for (final AppdefEntityID aeid : aeids) {
            final Resource resource = resourceManager.findResource(aeid);
            if (resource == null) {
                continue;
            }
            final List<Measurement> list = enabled.get(resource.getId());
            if (list != null && !list.isEmpty()) {
                toReschedule.add(aeid);
            } else {
                // if size() == 0 then resource was probably deleted
                // no measurements should be enabled
                toUnschedule.add(aeid);
            }
        }
    }

    private Set<AppdefEntityID> getOutOfSyncEntitiesToSchedule(Collection<SRN> srns, boolean overrideRestrictions) {
        final Set<AppdefEntityID> rtn = new HashSet<AppdefEntityID>();
        if (srns == null || srns.isEmpty()) {
            return rtn;
        }
        final boolean debug = log.isDebugEnabled();
        final long now = System.currentTimeMillis();
        for (final SRN srnObj : srns) {
            final AppdefEntityID aeid = srnObj.getEntity();
            if (aeid == null) {
                continue;
            }
            final Resource res = resourceManager.findResource(aeid);
            if (res == null || res.isInAsyncDeleteState()) {
                // ignore these
                continue;
            }
            final ScheduleRevNum srn = scheduleRevNumDAO.get(new SrnId(aeid));
            if (srn == null) {
                if (debug) log.debug("no srn associated with aeid=" + aeid + " scheduling");
                scheduleRevNumDAO.create(aeid);
                rtn.add(aeid);
            } else if (srn.getSrn() != srnObj.getRevisionNumber() && canSchedule(srn, now, overrideRestrictions)) {
                if (debug) {
                    log.debug("SRN value for " + aeid +
                              " is out of date, agent reports " + srnObj.getRevisionNumber() +
                              " but cached is " + srn.getSrn() + " rescheduling metrics..");
                }
                srn.setLastSchedule(now);
                rtn.add(aeid);
            }
        }
        return rtn;
    }

    @Transactional(readOnly=false)
    public void incrementSrns(Set<AppdefEntityID> aeids) {
        final long now = now();
        for (final AppdefEntityID aeid : aeids) {
            incrementSrn(aeid, now);
        }
    }
    
    private boolean canSchedule(ScheduleRevNum srn, long now, boolean overrideRestrictions) {
        if (overrideRestrictions) {
            return true;
        }
        if ((srn.getLastSchedule() + SCHEDULE_RESTRICTION) < now) {
            return true;
        }
        return false;
    }
    
    public void unscheduleNonEntities(String agentToken, Set<AppdefEntityID> aeids) {
        if (agentToken != null && !aeids.isEmpty()) {
            agentScheduleSynchronizer.unscheduleNonEntities(agentToken, aeids);
        }
    }

    @Transactional(readOnly=false)
    public void rescheduleOutOfSyncSrns(Collection<SRN> srnList, boolean overrideRestrictions) {
        final boolean debug = log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        if (debug) watch.markTimeBegin("setOutOfSyncEntities");
        final Set<AppdefEntityID> toReschedule = getOutOfSyncEntitiesToSchedule(srnList, overrideRestrictions);
        if (debug) watch.markTimeEnd("setOutOfSyncEntities");
        enqueueRescheduleEventAfterCommit(toReschedule);
        if (debug && watch.getElapsed() > 10) log.debug(watch);
    }

    private void enqueueRescheduleEventAfterCommit(Set<AppdefEntityID> toReschedule) {
        if (!toReschedule.isEmpty()) {
            zeventManager.enqueueEventAfterCommit(new AgentScheduleSyncZevent(toReschedule));
        }
    }

    public void setZeventManager(ZeventManager zeventManagerMock) {
        this.zeventManager = zeventManagerMock;
    }

    public void setMeasurementProcessor(MeasurementProcessor measurementProcessorMock) {
        this.measurementProcessor = measurementProcessorMock;
    }

    public ScheduleRevNum getSrnById(SrnId id) {
        return scheduleRevNumDAO.get(id);
    }

}
