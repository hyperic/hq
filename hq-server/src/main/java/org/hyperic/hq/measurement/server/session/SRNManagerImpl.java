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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import javax.persistence.EntityNotFoundException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.auth.domain.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.inventory.domain.Resource;
import org.hyperic.hq.measurement.MeasurementScheduleException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.data.MeasurementRepository;
import org.hyperic.hq.measurement.data.ScheduleRevNumRepository;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.hq.measurement.shared.SRNManager;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The tracker manager handles sending agents add and remove operations for the
 * log and config track plugsin.
 */
@Service
@Transactional
public class SRNManagerImpl implements SRNManager {
    private final Log log = LogFactory.getLog(SRNManagerImpl.class);

    private AuthzSubjectManager authzSubjectManager;
    private MeasurementManager measurementManager;
    private ScheduleRevNumRepository scheduleRevNumRepository;
    private SRNCache srnCache;
    private ResourceManager resourceManager;
    private MeasurementRepository measurementRepository;

    @Autowired
    public SRNManagerImpl(AuthzSubjectManager authzSubjectManager, MeasurementManager measurementManager,
                          ScheduleRevNumRepository scheduleRevNumRepository, SRNCache srnCache, ResourceManager resourceManager, 
                          MeasurementRepository measurementRepository) {
        this.authzSubjectManager = authzSubjectManager;
        this.measurementManager = measurementManager;
        this.scheduleRevNumRepository = scheduleRevNumRepository;
        this.srnCache = srnCache;
        this.resourceManager = resourceManager;
        this.measurementRepository = measurementRepository;
    }

    /**
     * Initialize the SRN Cache, or just return if it's already been
     * initialized.
     */
    public void initializeCache() {
        synchronized (srnCache) {
            if (srnCache.getSize() > 0) {
                return;
            }

            log.info("Initializing SRN Cache.");
            Collection<ScheduleRevNum> srns = scheduleRevNumRepository.findAll();
            log.info("Loaded " + srns.size() + " SRN entries.");
            for (ScheduleRevNum srn : srns) {
                srnCache.put(srn);
            }

            log.info("Fetching minimum metric collection intervals.");

            Collection<Object[]> entities = measurementRepository.getMinIntervals();
            log.info("Fetched " + entities.size() + " intervals.");
            final boolean debug = log.isDebugEnabled();
            for (Object[] ent : entities) {
                Resource resource = resourceManager.findResourceById((Integer)ent[0]);
                AppdefEntityID entityId = AppdefUtil.newAppdefEntityId(resource);
                SrnId id = new SrnId(entityId.getID());
                ScheduleRevNum srn = srnCache.get(id);
                if (srn == null) {
                    // Create the SRN if it does not exist.
                    srn = create(entityId.getID());
                    srnCache.put(srn);
                }
                if (debug) {
                    log.debug("Setting min interval to " + ((Long) ent[1]).longValue() + " for ent " + id);
                }
                srn.setMinInterval(((Long) ent[1]).longValue());
            }
        }
        log.info("SRN Cache initialized");
    }

    /**
     * Get a SRN
     * 
     * @param aid The entity id to lookup
     * @return The SRN for the given entity
     */
    @Transactional(readOnly=true)
    public ScheduleRevNum get(AppdefEntityID aid) {
        return srnCache.get(aid);
    }

    /**
     * Remove a SRN.
     * 
     * @param aid The AppdefEntityID to remove.
     */
    @Transactional(noRollbackFor=EntityNotFoundException.class)
    public void removeSrn(AppdefEntityID aid) {
        SrnId id = new SrnId(aid.getID());
        if (srnCache.remove(id)) {
            ScheduleRevNum srn = scheduleRevNumRepository.findById(id);
            if(srn != null) {
                scheduleRevNumRepository.delete(srn);
            }
        }
    }

    /**
     * Increment SRN for the given entity.
     * 
     * @param aid The AppdefEntityID to remove.
     * @param newMin The new minimum interval
     * @return The ScheduleRevNum for the given entity id
     */
    public int incrementSrn(AppdefEntityID aid, long newMin) {        
        SrnId id = new SrnId(aid.getID());
        ScheduleRevNum srn = scheduleRevNumRepository.findById(id);
        final boolean debug = log.isDebugEnabled();

        // Create the SRN if it does not already exist.
        if (srn == null) {
            // Create it
            if (debug) {
                log.debug("Creating SRN for appdef id=" + aid.getID());
            }
            srn = create(aid.getID());
            srnCache.put(srn);
            return srn.getSrn();
        }

        // Update SRN
        synchronized (srn) {
            int newSrn = srn.getSrn() + 1;
            if (debug) {
                log.debug("Updating SRN for " + aid + " to " + newSrn);
            }
            srn.setSrn(newSrn);

            if (newMin > 0 && newMin < srn.getMinInterval()) {
                srn.setMinInterval(newMin);
            } else {
                // Set to default
                Long defaultMin = measurementRepository.getMinInterval(aid.getId());
                // If this call to incrementSrn is due to the last metric
                // for a resource being unscheduled it's possible for
                // getMinInterval to return null if the session was flushed
                // before the SRN was deleted.
                if (defaultMin != null) {
                    srn.setMinInterval(defaultMin.longValue());
                }
            }
            srnCache.put(srn);
        }
        return srn.getSrn();
    }
    
    private ScheduleRevNum create(int resourceId) {
        SrnId srnId = new SrnId(resourceId);
        ScheduleRevNum srn = new ScheduleRevNum(srnId, 1);
        return scheduleRevNumRepository.save(srn);
    }

    /**
     * Handle a SRN report from an agent.
     * 
     * @param srns The list of SRNs from the agent report.
     * @return A Collection of ScheduleRevNum objects that do not have a
     *         corresponding appdef entity. (i.e. Out of sync)
     */
    public Collection<AppdefEntityID> reportAgentSRNs(SRN[] srns) {
        HashSet<AppdefEntityID> nonEntities = new HashSet<AppdefEntityID>();
        final boolean debug = log.isDebugEnabled();
        final List<AppdefEntityID> eids = new ArrayList<AppdefEntityID>(srns.length);

        for (int i = 0; i < srns.length; i++) {
            ScheduleRevNum srn = srnCache.get(srns[i].getEntity());

            if (srn == null) {
                log.error("Agent's reporting for non-existing entity: " + srns[i].getEntity());
                // TODO: Generics disagree with the comment; this adds
                // AppdefEntityID
                // and not ScheduleRevNum as the comment suggests
                nonEntities.add(srns[i].getEntity());
                continue;
            }

            synchronized (srn) {
                long current = System.currentTimeMillis();

                if (srns[i].getRevisionNumber() != srn.getSrn()) {
                    if (srn.getLastReported() > current - srn.getMinInterval()) {
                        // If the last reported time is less than an
                        // interval ago it could be that we just rescheduled
                        // the agent, so let's not panic yet
                        if (debug) {
                            log.debug("Ignore out-of-date SRN for grace " + "period of " + srn.getMinInterval());
                        }
                        break;
                    }

                    // SRN out of date, reschedule the metrics for the
                    // given resource.
                    if (debug) {
                        log.debug("SRN value for " + srns[i].getEntity() + " is out of date, agent reports " +
                                  srns[i].getRevisionNumber() + " but cached is " + srn.getSrn() +
                                  " rescheduling metrics..");
                    }
                   
                    eids.add(srns[i].getEntity());
                   
                }
                srn.setLastReported(current);
            }
        }
        if (!eids.isEmpty()) {
           AgentScheduleSyncZevent event = new AgentScheduleSyncZevent(eids);
            ZeventManager.getInstance().enqueueEventAfterCommit(event);
        }
        return nonEntities;
    }

    /**
     * Get a List of out-of-sync entities.
     * 
     * @return A list of ScheduleReNum objects that are out of sync.
     */
    @Transactional(readOnly=true)
    public List<AppdefEntityID> getOutOfSyncEntities() {
        List<ScheduleRevNum> srns = getOutOfSyncSRNs(3);
        ArrayList<AppdefEntityID> toReschedule = new ArrayList<AppdefEntityID>(srns.size());

        for (ScheduleRevNum srn : srns) {
            Resource resource = resourceManager.findResourceById(srn.getId().getInstanceId());
            AppdefEntityID eid = AppdefUtil.newAppdefEntityId(resource);
            // TODO: Generic disagree with comments
            toReschedule.add(eid);
        }
        return toReschedule;
    }

    /**
     * Get the list of out-of-sync SRNs based on the number of intervals back to
     * allow.
     * 
     * @param intervals The number of intervals to go back
     * @return A List of ScheduleRevNum objects.
     */
    @Transactional(readOnly=true)
    public List<ScheduleRevNum> getOutOfSyncSRNs(int intervals) {
        List<SrnId> srnIds = srnCache.getKeys();

        ArrayList<ScheduleRevNum> toReschedule = new ArrayList<ScheduleRevNum>();

        long current = System.currentTimeMillis();
        final boolean debug = log.isDebugEnabled();
        for (SrnId id : srnIds) {
            ScheduleRevNum srn = srnCache.get(id);

            long maxInterval = intervals * srn.getMinInterval();
            long curInterval = current - srn.getLastReported();
            if (debug) {
                log.debug("Checking " + ":" + id.getInstanceId() + ", last heard from " +
                          curInterval + "ms ago (max=" + maxInterval + ")");
            }

            if (curInterval > maxInterval) {
                if (debug) {
                    log.debug("Reschedule " + id.getInstanceId());
                }
                toReschedule.add(srn);
            }
        }

        return toReschedule;
    }

    /**
     * Refresh the SRN for the given entity.
     * 
     * @param eid The appdef entity to refresh
     * @return The new ScheduleRevNum object.
     */
    public ScheduleRevNum refreshSRN(AppdefEntityID eid) {
        ScheduleRevNum srn = create( eid.getID());

        srnCache.put(srn);

        Long min = measurementRepository.getMinInterval(eid.getId());
        srn.setMinInterval(min.longValue());

        srnCache.put(srn);

        return srn;
    }

    /**
     * Reschedule metrics for an appdef entity. Generally should only be called
     * from the {@link AgentScheduleSynchronizer}
     * @param List of {@link AppdefEntityId}
     */
    public void reschedule(List<AppdefEntityID> aeids) throws MeasurementScheduleException, MonitorAgentException,
        MeasurementUnscheduleException {
        AuthzSubject subj = authzSubjectManager.getOverlordPojo();
        List<AppdefEntityID> toReschedule = new ArrayList<AppdefEntityID>();
        List<AppdefEntityID> toUnschedule = new ArrayList<AppdefEntityID>();
        for (AppdefEntityID aeid : aeids) {
            // will return only enabled measurements
            List<Measurement> meas = measurementManager.findMeasurements(subj, aeid, null, PageControl.PAGE_ALL);
            if (meas.size() > 0) {
                toReschedule.add(aeid);
            } else {
                // if size() == 0 then resource was probably deleted
                // no measurements should be enabled
                toUnschedule.add(aeid);
            }
        }
        measurementManager.scheduleSynchronous(toReschedule);
        measurementManager.unschedule(toUnschedule);
    }
}
