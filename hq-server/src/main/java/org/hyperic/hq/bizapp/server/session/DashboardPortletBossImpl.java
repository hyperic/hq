/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2009], Hyperic, Inc.
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

package org.hyperic.hq.bizapp.server.session;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManager;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.authz.shared.ResourceManager;
import org.hyperic.hq.bizapp.shared.DashboardPortletBoss;
import org.hyperic.hq.bizapp.shared.MeasurementBoss;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertSortField;
import org.hyperic.hq.events.shared.AlertDefinitionManager;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertManager;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.galerts.shared.GalertManager;
import org.hyperic.hq.grouping.server.session.GroupUtil;
import org.hyperic.hq.grouping.shared.GroupNotCompatibleException;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.shared.DataManager;
import org.hyperic.hq.measurement.shared.HighLowMetricValue;
import org.hyperic.hq.measurement.shared.MeasurementManager;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.timer.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 */
@Service
@Transactional
public class DashboardPortletBossImpl implements DashboardPortletBoss {

    private static final String ALERT_CRITICAL = "red", ALERT_WARN = "yellow", ALERT_UNKNOWN = "gray",
        ALERT_OK = "green";

    private final Log log = LogFactory.getLog(DashboardPortletBossImpl.class);

    private PermissionManager permissionManager;

    private ResourceManager resourceManager;

    private MeasurementManager measurementManager;

    private DataManager dataManager;

    private MeasurementBoss measurementBoss;

    private ResourceGroupManager resourceGroupManager;

    private GalertManager galertManager;

    private AlertManager alertManager;

    private AlertDefinitionManager alertDefinitionManager;

    private EscalationManager escalationManager;
    
    private AlertPermissionManager alertPermissionManager;

    @Autowired
    public DashboardPortletBossImpl(PermissionManager permissionManager, ResourceManager resourceManager,
                                    MeasurementManager measurementManager, DataManager dataManager,
                                    MeasurementBoss measurementBoss, ResourceGroupManager resourceGroupManager,
                                    GalertManager galertManager, AlertManager alertManager,
                                    AlertDefinitionManager alertDefinitionManager, EscalationManager escalationManager, 
                                    AlertPermissionManager alertPermissionManager) {
        this.permissionManager = permissionManager;
        this.resourceManager = resourceManager;
        this.measurementManager = measurementManager;
        this.dataManager = dataManager;
        this.measurementBoss = measurementBoss;
        this.resourceGroupManager = resourceGroupManager;
        this.galertManager = galertManager;
        this.alertManager = alertManager;
        this.alertDefinitionManager = alertDefinitionManager;
        this.escalationManager = escalationManager;
        this.alertPermissionManager = alertPermissionManager;
    }
    
    @Transactional(readOnly=true)
    public List<Map<String, Object>> getMeasurementData(AuthzSubject subj, Integer resId, Integer mtid, AppdefEntityTypeID ctype,
                                        long begin, long end) throws PermissionException {
        List<Map<String, Object>> result = new ArrayList<Map<String,Object>>();

        DateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);
        long intv = (end - begin) / 60;
        Map<String, Object> chartData = new HashMap<String, Object>();
        Resource res = resourceManager.findResourceById(resId);
        
        if (res == null || res.isInAsyncDeleteState()) {
            return result;
        }
        
        AppdefEntityID aeid = AppdefUtil.newAppdefEntityId(res);
        
        try {
        	chartData.put("resourceName", res.getName());

            AppdefEntityID[] aeids;
            
            if (aeid.isGroup()) {
                List<AppdefEntityID> members = GroupUtil.getCompatGroupMembers(subj, aeid, null, PageControl.PAGE_ALL);
            
                aeids = (AppdefEntityID[]) members.toArray(new AppdefEntityID[members.size()]);
            } else if (ctype != null) {
                aeids = measurementBoss.getAutoGroupMemberIDs(subj, new AppdefEntityID[] { aeid }, ctype);
            } else {
                aeids = new AppdefEntityID[] { aeid };
            }
            
            List<Measurement> metrics = measurementManager.findMeasurements(subj, mtid, aeids);

            // Get measurement name
            if (!metrics.isEmpty()) {
                Measurement measurement = metrics.get(0);
                
                chartData.put("measurementName", measurement.getTemplate().getName());
                chartData.put("measurementUnits", measurement.getTemplate().getUnits());
            }

            List<HighLowMetricValue> data = dataManager.getHistoricalData(metrics, begin, end, intv, 0, true,
                PageControl.PAGE_ALL);

            Map<String, List<Double>> metricData = new LinkedHashMap<String, List<Double>>();
            
            for (HighLowMetricValue pt : data) {
                List<Double> metricValues = new ArrayList<Double>();
                double val = pt.getValue();
                
                if (Double.isNaN(val) || Double.isInfinite(val)) {
                    continue;
                }
                
                metricValues.add(val);
                
                Date date = new Date(pt.getTimestamp());
                
                metricData.put(dateFmt.format(date), metricValues);
            }

            chartData.put("data", metricData);
            
            result.add(chartData);
        } catch (AppdefEntityNotFoundException e) {
            log.error("AppdefEntityNotFound: " + aeid);
        } catch (GroupNotCompatibleException e) {
            log.error("GroupNotCompatibleException: " + aeid);
        }
        
        return result;
    }

    @Transactional(readOnly=true)
    public Map<Integer, List<String>> getAlertCounts(AuthzSubject subj, Integer[] groupIds, PageInfo pageInfo)
        throws PermissionException {
        final long PORTLET_RANGE = MeasurementConstants.DAY * 3;
        final int maxRecords = pageInfo.getStartRow() + pageInfo.getPageSize();

        Map<Integer, List<Alert>> resourceAlertMap =
                alertManager.getUnfixedByResource(subj.getId(), PORTLET_RANGE, System.currentTimeMillis());

        Map<Integer, List<String>> result = new HashMap<Integer, List<String>>();
        for (int x = pageInfo.getStartRow(); x < groupIds.length && (maxRecords == 0 || x <= maxRecords); x++) {
            ResourceGroup group = resourceGroupManager.findResourceGroupById(subj, groupIds[x]);
            
            if (group != null) {
            	List<String> alertStatus = new ArrayList<String>();
                alertStatus.add(getResourceStatus(subj, group, resourceAlertMap, PORTLET_RANGE));
            	alertStatus.add(getGroupStatus(subj, group, PORTLET_RANGE));
            	result.put(group.getId(), alertStatus);
            }
        }
        
        return result;
    }

    private String getGroupStatus(AuthzSubject subj, ResourceGroup group, long range) {
        boolean debug = log.isDebugEnabled();
        String rtn = ALERT_OK;
        long now = System.currentTimeMillis();

        try {
            long begin = now - range;

            List<GalertLog> galerts = galertManager.findUnfixedAlertLogsByTimeWindow(group, begin, now);
            if (debug) {
                log.debug("getGroupStatus: findUnfixedAlertLogsByTimeWindow execution time(ms)=" +
                          (System.currentTimeMillis() - now));
            }

            for (GalertLog galert : galerts) {

                try {
                    alertPermissionManager.canViewAlertDefinition(subj, galert.getAlertDef().getAppdefID());
                } catch (PermissionException pe) {
                    // continue to next group alert
                    continue;
                }
                // a galert always has an associated escalation which may or may
                // not
                // be acknowledged.
                if (galert.hasEscalationState() && galert.isAcknowledged()) {
                    rtn = ALERT_WARN;
                } else {
                    return ALERT_CRITICAL;
                }
            }

            // Is it that there are no alerts or that there are no alert
            // definitions?
            if (rtn.equals(ALERT_OK)) {
                List<GalertDef> galertDefs = galertManager.findAlertDefs(group, PageControl.PAGE_ALL);
                if (galertDefs.size() == 0) {
                    return ALERT_UNKNOWN;
                }
            }

            return rtn;
        } finally {
            if (debug) {
                log.debug("getGroupStatus: groupId=" + group.getId() + ", execution time(ms)=" +
                          (System.currentTimeMillis() - now));
            }
        }
    }

    private String getResourceStatus(AuthzSubject subj, ResourceGroup group,
                                     Map<Integer,List<Alert>>resourceAlertMap,
                                     long range) {
        boolean debug = log.isDebugEnabled();
        long now = System.currentTimeMillis();
        StopWatch watch = new StopWatch(now);

        try {
            watch.markTimeBegin("getResourceStatus: getUnfixedCount");
            Collection<Resource> resources = resourceGroupManager.getMembers(group);
            List<Alert> alerts = new ArrayList<Alert>();
            for (Resource r : resources) {
                List<Alert> resourceAlerts = resourceAlertMap.get(r.getId());
                if (resourceAlerts != null) {
                    alerts.addAll(resourceAlerts);
                }
            }
            watch.markTimeEnd("getResourceStatus: getUnfixedCount");

            // There are unfixed alerts for resources in this group.
            if (alerts.size() > 0) {
                // If all alerts are ack'ed, return WARN, otherwise CRITICAL.
                for (Alert alert : alerts) {
                    if (!isAckd(subj, alert)) {
                        return ALERT_CRITICAL;
                    }
                }
                return ALERT_WARN;
            } else {
                // Is it that there are no alerts or that there are no alert definitions?
                // TODO: Should query these all at once - once complete we can
                // remove the query cache for AlertDefinition.findByResource.  This
                // used to check alerting permission, but now that's already done
                // on the master list of alerts.
                List<AlertDefinition> alertDefs;
                for (Resource r : resources) {
                    alertDefs = alertDefinitionManager.findAlertDefinitions(subj, r);
                    if (alertDefs.size() > 0) {
                        return ALERT_OK;
                    }
                }
            }
        } catch (PermissionException e) {
            // User has no permission to see these resources
        } finally {
            if (debug) {
                log.debug("getResourceStatus: groupId=" + group.getId() + ", execution time =" + watch);
            }
        }
        return ALERT_UNKNOWN;
    }

    private boolean isAckd(AuthzSubject subj, Alert alert) {
        AlertDefinition alertDef = alert.getAlertDefinition();
        // a resource alert may not have an associated escalation
        Escalation esc = alertDef.getEscalation();
        if (esc == null || esc.getMaxPauseTime() == 0) {
            return false;
        }

        EscalationState state = escalationManager.findEscalationState(alertDef);
        return state != null && state.getAcknowledgedBy() != null;
    }

}
