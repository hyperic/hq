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

package org.hyperic.hq.measurement.server.session;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.Agent;
import org.hyperic.hq.appdef.server.session.Platform;
import org.hyperic.hq.appdef.server.session.PlatformManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Server;
import org.hyperic.hq.appdef.server.session.ServerManagerEJBImpl;
import org.hyperic.hq.appdef.server.session.Service;
import org.hyperic.hq.appdef.server.session.ServiceManagerEJBImpl;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.PlatformNotFoundException;
import org.hyperic.hq.appdef.shared.ServerNotFoundException;
import org.hyperic.hq.appdef.shared.ServiceNotFoundException;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.MeasurementUnscheduleException;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.data.DSNList;
import org.hyperic.hq.measurement.data.MeasurementReport;
import org.hyperic.hq.measurement.data.ValueList;
import org.hyperic.hq.measurement.monitor.MonitorAgentException;
import org.hyperic.hq.measurement.shared.MeasurementManagerLocal;
import org.hyperic.hq.measurement.shared.ReportProcessorLocal;
import org.hyperic.hq.measurement.shared.ReportProcessorUtil;
import org.hyperic.hq.measurement.shared.SRNManagerLocal;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.util.StringUtil;
import org.hyperic.util.timer.StopWatch;

/**
 * @ejb:bean name="ReportProcessor"
 *      jndi-name="ejb/measurement/ReportProcessor"
 *      local-jndi-name="LocalReportProcessor"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:transaction type="Required"
 */
public class ReportProcessorEJBImpl 
    extends SessionEJB 
    implements SessionBean 
{
    private final Log _log = LogFactory.getLog(ReportProcessorEJBImpl.class);

    private final long MINUTE = MeasurementConstants.MINUTE;
    private final long PRIORITY_OFFSET = MINUTE*3;
    
    private void addPoint(List points, List priorityPts, Measurement m,
                          MetricValue[] vals)
    {
        final boolean debug = _log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        for (int i=0; i<vals.length; i++)
        {
            final long now =
                TimingVoodoo.roundDownTime(System.currentTimeMillis(), MINUTE);
            try {
                //this is just to check if the metricvalue is valid
                //will throw a NumberFormatException if there is a problem
                new BigDecimal(vals[i].getValue());
                DataPoint pt = new DataPoint(m.getId(), vals[i]);
                if (priorityPts != null && isPriority(now, pt.getTimestamp())) {
                    priorityPts.add(pt);
                } else {
                    points.add(pt);
                }
                if (debug) watch.markTimeBegin("getTemplate");
                if (debug && m.getTemplate().isAvailability()) {
                    _log.debug("availability -> " + pt);
                }
                if (debug) watch.markTimeEnd("getTemplate");
            } catch(NumberFormatException e) {
                _log.warn("Unable to insert: " + e.getMessage() +
                          ", metric id=" + m);
            }
        }
        if (debug) _log.debug(watch);
    }

    private final boolean isPriority(long timestamp, long metricTimestamp) {
        if (metricTimestamp >= (timestamp-PRIORITY_OFFSET)) {
            return true;
        }
        return false;
    }

    private final void addData(List points, List priorityPts, Measurement m,
                               MetricValue[] dpts) {
        final boolean debug = _log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        if (debug) watch.markTimeBegin("getInterval");
        long interval = m.getInterval();
        if (debug) watch.markTimeEnd("getInterval");

        // Safeguard against an anomaly
        if (interval <= 0) {
            _log.warn("Measurement had bogus interval[" + interval + "]: " + m);
            interval = 60 * 1000;
        }

        // Each datapoint corresponds to a set of measurement
        // values for that cycle.
        MetricValue[] passThroughs = new MetricValue[dpts.length];

        for (int i = 0; i < dpts.length; i++) {
            // Save data point to DB.
            if (debug) watch.markTimeBegin("getTimestamp");
            long retrieval = dpts[i].getTimestamp();
            if (debug) watch.markTimeEnd("getTimestamp");
            if (debug) watch.markTimeBegin("roundDownTime");
            long adjust = TimingVoodoo.roundDownTime(retrieval, interval);
            if (debug) watch.markTimeEnd("roundDownTime");

            // Create new Measurement data point with the adjusted time
            if (debug) watch.markTimeBegin("new MetricValue");
            MetricValue modified = new MetricValue(dpts[i].getValue(), adjust);
            if (debug) watch.markTimeEnd("new MetricValue");
            passThroughs[i] = modified;
        }
        if (debug) watch.markTimeBegin("addPoint");
        addPoint(points, priorityPts, m, passThroughs);
        if (debug) watch.markTimeEnd("addPoint");
        if (debug) _log.debug(watch);
    }

    /**
     * Method which takes data from the agent (or elsewhere) and throws
     * it into the DataManager, doing the right things with all the
     * derived measurements
     *
     * @ejb:interface-method
     */
    public void handleMeasurementReport(MeasurementReport report)
        throws DataInserterException
    {
        final DSNList[] dsnLists = report.getClientIdList();
        final String agentToken = report.getAgentToken();

        final List dataPoints = new ArrayList(dsnLists.length);
        final List availPoints = new ArrayList(dsnLists.length);
        final List priorityAvailPts = new ArrayList(dsnLists.length);
        
        final MeasurementManagerLocal mMan = MeasurementManagerEJBImpl.getOne();
        final boolean debug = _log.isDebugEnabled();
        final StopWatch watch = new StopWatch();
        if (debug) watch.markTimeBegin("verifyAndAddData");
        for (int i = 0; i < dsnLists.length; i++) {
            Integer dmId = new Integer(dsnLists[i].getClientId());
            Measurement m = mMan.getMeasurement(dmId);
            
            // Can't do much if we can't look up the derived measurement
            // If the measurement is enabled, we just throw away their data
            // instead of trying to throw it into the backfill.  This is 
            // because we don't know the interval to normalize those old
            // points for.  This is still a problem for people who change their
            // collection period, but the instances should be low.
            if (m == null || !m.isEnabled()) {
                if (debug) {
                    if (m == null) {
                        _log.debug("dropping metricId=" + dmId
                                    + " because measurement is null");
                    } else {
                        _log.debug("dropping metricId=" + dmId
                                    + " because measurement is disabled");
                    }
                }
                continue;
            }
            // Need to check if resource was asynchronously deleted (type == null)
            final Resource res = m.getResource();
            if (res == null || res.isInAsyncDeleteState()) {
                if (debug) {
                    _log.debug("dropping metricId=" + m.getId() +
                        " since resource is in async delete state");
                }
                continue;
            }
            if (!resourceMatchesAgent(res, agentToken)) {
                String ipAddr = "<Unknown IP address>";
                String portString = "<Unknown port>";
                try {
                    Agent agt = getAgent(agentToken);
                    ipAddr = agt.getAddress();
                    portString = agt.getPort().toString();
                } catch (MonitorAgentException mae) {
                    // leave values as default
                    _log.debug("Error trying to construct string for WARN message below", mae);
                }
                
                _log.warn("measurement (id=" + m.getId() + ", name=" +
                    m.getTemplate().getName() + ") was sent to the " +
                    "HQ server from agent (agentToken=" + agentToken + ", name=" +
                    ipAddr + ", port=" + portString + ")" +
                    " but resource (id=" + res.getId() + ", name=" +
                    res.getName() + ") is not associated " +
                    " with that agent.  Dropping measurement.");
                continue;
            }

            final boolean isAvail = m.getTemplate().isAvailability();
            final ValueList[] valLists = dsnLists[i].getDsns();
            for (int j = 0; j < valLists.length; j++) {
                final MetricValue[] vals = valLists[j].getValues();
                if (isAvail) {
                    addData(availPoints, priorityAvailPts, m, vals);
                } else {
                    addData(dataPoints, null, m, vals);
                }
            }
        }
        if (debug) {
            watch.markTimeEnd("verifyAndAddData");
            _log.debug("received "
                        + dsnLists.length 
                        + " metrics, processing "
                        + dataPoints.size() + " data points, "
                        + availPoints.size() + " avail points, " 
                        + priorityAvailPts.size() 
                        + " priority avail points from agent with token " 
                        + agentToken);
        }

        DataInserter d = MeasurementStartupListener.getDataInserter();
        if (debug) watch.markTimeBegin("sendMetricDataToDB");
        sendMetricDataToDB(d, dataPoints, false);
        if (debug) watch.markTimeEnd("sendMetricDataToDB");
        DataInserter a = MeasurementStartupListener.getAvailDataInserter();
        if (debug) watch.markTimeBegin("sendAvailDataToDB");
        sendMetricDataToDB(a, availPoints, false);
        sendMetricDataToDB(a, priorityAvailPts, true);
        if (debug) watch.markTimeEnd("sendAvailDataToDB");

        // Check the SRNs to make sure the agent is up-to-date
        SRNManagerLocal srnManager = getSRNManager();
        if (debug) watch.markTimeBegin("reportAgentSRNs");
        Collection nonEntities = srnManager.reportAgentSRNs(report.getSRNList());
        if (debug) watch.markTimeEnd("reportAgentSRNs");
        
        if (report.getAgentToken() != null && nonEntities.size() > 0) {
            // Better tell the agent to stop reporting non-existent entities
            AppdefEntityID[] entIds = (AppdefEntityID[])
                nonEntities.toArray(new AppdefEntityID[nonEntities.size()]);
            try {
                if (debug) watch.markTimeBegin("unschedule");
                MeasurementProcessorEJBImpl.getOne().unschedule(
                    report.getAgentToken(), entIds);
                if (debug) watch.markTimeEnd("unschedule");
            } catch (MeasurementUnscheduleException e) {
                _log.error("Cannot unschedule entities: " +
                          StringUtil.arrayToString(entIds));
            }
        }
        if (debug) _log.debug("handleMeasurementReport: " + watch);
    }

    /**
     * checks if the agentToken matches resource's agentToken
     */
    private boolean resourceMatchesAgent(Resource resource, String agentToken) {
        if (resource == null || resource.isInAsyncDeleteState()) {
            return false;
        }
        final Integer resType = resource.getResourceType().getId();
        final Integer aeid = resource.getInstanceId();
        try {
            if (resType.equals(AuthzConstants.authzPlatform)) {
                Platform p =
                    PlatformManagerEJBImpl.getOne().findPlatformById(aeid);
                Resource r = p.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    return false;
                }
                Agent a = p.getAgent();
                if (a == null) {
                    return false;
                }
                return a.getAgentToken().equals(agentToken);
            } else if (resType.equals(AuthzConstants.authzServer)) {
                Server server =
                    ServerManagerEJBImpl.getOne().findServerById(aeid);
                Resource r = server.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    return false;
                }
                Platform p = server.getPlatform();
                if (p == null) {
                    return false;
                }
                r = p.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    return false;
                }
                Agent a = p.getAgent();
                if (a == null) {
                    return false;
                }
                return a.getAgentToken().equals(agentToken);
            } else if (resType.equals(AuthzConstants.authzService)) {
                Service service =
                    ServiceManagerEJBImpl.getOne().findServiceById(aeid);
                Resource r = service.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    return false;
                }
                Server server = service.getServer();
                if (server == null) {
                    return false;
                }
                r = server.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    return false;
                }
                Platform p = server.getPlatform();
                if (p == null) {
                    return false;
                }
                r = p.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    return false;
                }
                Agent a = p.getAgent();
                if (a == null) {
                    return false;
                }
                return a.getAgentToken().equals(agentToken);
            }
        } catch (PlatformNotFoundException e) {
            _log.warn("Platform not found Id=" + aeid);
        } catch (ServerNotFoundException e) {
            _log.warn("Server not found Id=" + aeid);
        } catch (ServiceNotFoundException e) {
            _log.warn("Service not found Id=" + aeid);
        }
        return false;
    }

    /**
     * Sends the actual data to the DB.
     */
    private void sendMetricDataToDB(DataInserter d, List dataPoints,
                                    boolean isPriority) 
        throws DataInserterException
    {
        if (dataPoints.size() <= 0) {
            return;
        }
        try {
            d.insertMetrics(dataPoints, isPriority);
            int size = dataPoints.size();
            long ts = System.currentTimeMillis();
            ReportStatsCollector.getInstance().getCollector().add(size, ts);
        } catch(InterruptedException e) {
            throw new SystemException("Interrupted while attempting to " + 
                                      "insert data");
        }
    }

    public static ReportProcessorLocal getOne() {
        try {
            return ReportProcessorUtil.getLocalHome().create();
        } catch (Exception e) {
            throw new SystemException(e);
        }
    }
    
    public void ejbCreate(){}
    public void ejbPostCreate(){}
    public void ejbActivate(){}
    public void ejbPassivate(){}
    public void ejbRemove(){}
    public void setSessionContext(SessionContext ctx){}
} 
