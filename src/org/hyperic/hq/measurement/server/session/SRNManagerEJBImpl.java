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
import java.util.Iterator;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementScheduleException;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.SRNManagerLocal;
import org.hyperic.hq.measurement.shared.SRNManagerUtil;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.pager.PageControl;

/**
 * The tracker manager handles sending agents add and remove operations
 * for the log and config track plugsin.
 *
 * @ejb:bean name="SRNManager"
 *      jndi-name="ejb/measurement/SRNManager"
 *      local-jndi-name="SRNManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:transaction type="Required"
 */
public class SRNManagerEJBImpl extends SessionEJB
    implements SessionBean {

    private final Log _log = LogFactory.getLog(SRNManagerEJBImpl.class);

    /**
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {}

    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}

    /**
     * Initialize the SRN Cache, or just return if it's already been
     * initialized.
     *
     * @ejb:interface-method
     */
    public void initializeCache() {
        SRNCache cache = SRNCache.getInstance();

        synchronized(cache) {
            if (cache.getSize() > 0) {
                return;
            }

            _log.info("Initializing SRN Cache.");
            Collection srns = getScheduleRevNumDAO().findAll();
            _log.info("Loaded " + srns.size() + " SRN entries.");
            for (Iterator i = srns.iterator(); i.hasNext(); ) {
                ScheduleRevNum srn = (ScheduleRevNum)i.next();
                cache.put(srn);
            }

            _log.info("Fetching minimum metric collection intervals.");

            Collection entities =
                getScheduleRevNumDAO().getMinIntervals();
            _log.info("Fetched " + entities.size() + " intervals.");
            final boolean debug = _log.isDebugEnabled();
            for (Iterator i = entities.iterator(); i.hasNext(); ) {
                Object[] ent = (Object[])i.next();
                SrnId id = new SrnId(((Integer)ent[0]).intValue(),
                                     ((Integer)ent[1]).intValue());
                ScheduleRevNum srn = cache.get(id);
                if (srn == null) {
                    // Create the SRN if it does not exist.
                    srn = getScheduleRevNumDAO()
                        .create(((Integer)ent[0]).intValue(),
                                ((Integer)ent[1]).intValue());
                    cache.put(srn);
                }
                if (debug) {
                    _log.debug("Setting min interval to " +
                               ((Long)ent[2]).longValue() + " for ent " + id);
                }
                srn.setMinInterval(((Long)ent[2]).longValue());
            }
        }
        _log.info("SRN Cache initialized");
    }

    /**
     *  Get a SRN
     *
     * @ejb:interface-method
     * @param aid The entity id to lookup
     * @return The SRN for the given entity
     */
    public ScheduleRevNum get(AppdefEntityID aid) {
        SRNCache cache = SRNCache.getInstance();
        return cache.get(aid);
    }

    /**
     * Remove a SRN.
     *
     * @ejb:interface-method
     * @param aid The AppdefEntityID to remove.
     */
    public void removeSrn(AppdefEntityID aid) {
        SRNCache cache = SRNCache.getInstance();
        SrnId id = new SrnId(aid.getType(), aid.getID());
        if (cache.remove(id)) {
            getScheduleRevNumDAO().remove(id);
        }
    }

    /**
     * Increment SRN for the given entity.
     *
     * @ejb:interface-method
     * @param aid The AppdefEntityID to remove.
     * @param newMin The new minimum interval
     * @return The ScheduleRevNum for the given entity id
     */
    public int incrementSrn(AppdefEntityID aid, long newMin) {
        SRNCache cache = SRNCache.getInstance();
        ScheduleRevNumDAO dao = getScheduleRevNumDAO();
        
        SrnId id = new SrnId(aid.getType(), aid.getID());
        ScheduleRevNum srn = dao.get(id);
        final boolean debug = _log.isDebugEnabled();

        // Create the SRN if it does not already exist.
        if (srn == null) {
            // Create it
            if (debug) {
                _log.debug("Creating SRN for appdef id=" + aid.getID());
            }
            srn = dao.create(aid.getType(), aid.getID());
            cache.put(srn);
            return srn.getSrn();
        }

        // Update SRN
        synchronized(srn) {
            int newSrn = srn.getSrn() + 1;
            if (debug) {
                _log.debug("Updating SRN for "+ aid + " to " + newSrn);
            }
            srn.setSrn(newSrn);

            if (newMin > 0 && newMin < srn.getMinInterval()) {
                srn.setMinInterval(newMin);
            } else {
            // Set to default
                Long defaultMin = dao.getMinInterval(aid, true);
                // If this call to incrementSrn is due to the last metric
                // for a resource being unscheduled it's possible for
                // getMinInterval to return null if the session was flushed
                // before the SRN was deleted.
                if (defaultMin != null) {
                    srn.setMinInterval(defaultMin.longValue());
                }
            }

            cache.put(srn);
        }
        return srn.getSrn();
    }

    /**
     * Handle a SRN report from an agent.
     *
     * @ejb:interface-method
     * @param srns The list of SRNs from the agent report.
     * @return A Collection of ScheduleRevNum objects that do not have a
     * corresponding appdef entity. (i.e. Out of sync)
     */
    public Collection reportAgentSRNs(SRN[] srns) {
        SRNCache cache = SRNCache.getInstance();
        HashSet nonEntities = new HashSet();
        final boolean debug = _log.isDebugEnabled();
        final List eids = new ArrayList(srns.length);

        for (int i = 0; i < srns.length; i++) {
            ScheduleRevNum srn = cache.get(srns[i].getEntity());

            if (srn == null) {
                _log.error("Agent's reporting for non-existing entity: " + srns[i].getEntity());
                nonEntities.add(srns[i].getEntity());
                continue;
            }

            synchronized (srn) {
                long current = System.currentTimeMillis();

                if (srns[i].getRevisionNumber() != srn.getSrn()) {
                    if (srn.getLastReported() >
                        current - srn.getMinInterval()) {
                        // If the last reported time is less than an
                        // interval ago it could be that we just rescheduled
                        // the agent, so let's not panic yet
                        if (debug) {
                            _log.debug("Ignore out-of-date SRN for grace " +
                                       "period of " + srn.getMinInterval());
                        }
                        break;
                    }

                    // SRN out of date, reschedule the metrics for the
                    // given resource.
                    if (debug) {
                        _log.debug("SRN value for " + srns[i].getEntity() +
                                   " is out of date, agent reports " +
                                   srns[i].getRevisionNumber() +
                                   " but cached is " + srn.getSrn() +
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
     * @ejb:interface-method
     * @return A list of ScheduleReNum objects that are out of sync.
     */
    public List getOutOfSyncEntities() {
        List srns = getOutOfSyncSRNs(3);
        ArrayList toReschedule = new ArrayList(srns.size());

        for (Iterator it = srns.iterator(); it.hasNext(); ) {
            ScheduleRevNum srn = (ScheduleRevNum)it.next();

            AppdefEntityID eid =
                new AppdefEntityID(srn.getId().getAppdefType(),
                                   srn.getId().getInstanceId());
            toReschedule.add(eid);
        }

        return toReschedule;
    }

    /**
     * Get the list of out-of-sync SRNs based on the number of intervals back
     * to allow.
     *
     * @ejb:interface-method
     * @param intervals The number of intervals to go back
     * @return A List of ScheduleRevNum objects.
     */
    public List getOutOfSyncSRNs(int intervals) {

        SRNCache cache = SRNCache.getInstance();
        List srnIds = cache.getKeys();

        ArrayList toReschedule = new ArrayList();

        long current = System.currentTimeMillis();
        final boolean debug = _log.isDebugEnabled();
        for (Iterator i = srnIds.iterator(); i.hasNext(); ) {

            SrnId id = (SrnId)i.next();
            ScheduleRevNum srn = cache.get(id);

            long maxInterval = intervals * srn.getMinInterval();
            long curInterval = current - srn.getLastReported();
            if (debug) {
                _log.debug("Checking " + id.getAppdefType() + ":" +
                           id.getInstanceId() + ", last heard from " +
                           curInterval + "ms ago (max=" + maxInterval + ")");
            }

            if (curInterval > maxInterval) {
                if (debug) {
                    _log.debug("Reschedule " + id.getAppdefType() + ":" +
                               id.getInstanceId());
                }
                toReschedule.add(srn);
            }
        }

        return toReschedule;
    }

    /**
     * Refresh the SRN for the given entity.
     *
     * @ejb:interface-method
     * @param eid The appdef entity to refresh
     * @return The new ScheduleRevNum object.
     */
    public ScheduleRevNum refreshSRN(AppdefEntityID eid) {
        SRNCache cache = SRNCache.getInstance();
        ScheduleRevNumDAO dao = getScheduleRevNumDAO();
        ScheduleRevNum srn = dao.create(eid.getType(), eid.getID());

        cache.put(srn);

        Long min = dao.getMinInterval(eid);
        srn.setMinInterval(min.longValue());

        cache.put(srn);

        return srn;
    }
    
    /**
     * Reschedule metrics for an appdef entity.  Generally should only
     * be called from the {@link AgentScheduleSynchronizer}
     * @param List of {@link AppdefEntityId}
     *
     * @ejb:interface-method
     */
    public void reschedule(List aeids)
        throws MeasurementScheduleException,
               MonitorAgentException,
               MeasurementUnscheduleException
    {
        AuthzSubject subj = AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
        MeasurementManagerLocal mMan = MeasurementManagerEJBImpl.getOne();
        List toReschedule = new ArrayList();
        List toUnschedule = new ArrayList();
        for (Iterator it=aeids.iterator(); it.hasNext(); ) {
            AppdefEntityID aeid = (AppdefEntityID)it.next();
            // will return only enabled measurements
            List meas =
                mMan.findMeasurements(subj, aeid, null, PageControl.PAGE_ALL);
            if (meas.size() > 0) {
                toReschedule.add(aeid);
            } else {
                // if size() == 0 then resource was probably deleted
                // no measurements should be enabled
                toUnschedule.add(aeid);
            }
        }
        MeasurementProcessorEJBImpl.getOne().scheduleSynchronous(toReschedule);
        MeasurementProcessorEJBImpl.getOne().unschedule(toUnschedule);
    }
    
    
    public static SRNManagerLocal getOne() {
        try {
            return SRNManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
}
