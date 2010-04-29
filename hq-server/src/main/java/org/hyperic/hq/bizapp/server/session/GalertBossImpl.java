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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefUtil;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.ResourceGroupManager;
import org.hyperic.hq.bizapp.shared.GalertBoss;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.AlertPermissionManager;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyInfo;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyType;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfo;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.galerts.server.session.GtriggerType;
import org.hyperic.hq.galerts.server.session.GtriggerTypeInfo;
import org.hyperic.hq.galerts.shared.GalertManager;
import org.hyperic.hq.galerts.shared.GtriggerManager;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The BizApp's interface to the Events Subsystem
 */
@Service
@Transactional
public class GalertBossImpl implements GalertBoss {
    private SessionManager sessionManager;
    private GalertManager galertManager;
    private GtriggerManager gtriggerManager;
    private AlertPermissionManager alertPermissionManager;
    private ResourceGroupManager resourceGroupManager;
    private EscalationManager escalationManager;

    @Autowired
    public GalertBossImpl(GalertManager galertManager,
                          GtriggerManager gtriggerManager,
                          AlertPermissionManager alertPermissionManager,
                          SessionManager sessionManager,
                          ResourceGroupManager resourceGroupManager,
                          EscalationManager escalationManager) {
        this.galertManager = galertManager;
        this.gtriggerManager = gtriggerManager;
        this.alertPermissionManager = alertPermissionManager;
        this.sessionManager = sessionManager;
        this.resourceGroupManager = resourceGroupManager;
        this.escalationManager = escalationManager;
    }

    /**
     */
    public ExecutionStrategyTypeInfo registerExecutionStrategy(int sessionId,
                                                               ExecutionStrategyType stratType)
        throws PermissionException, SessionException {
        sessionManager.authenticate(sessionId);
        return galertManager.registerExecutionStrategy(stratType);

    }

    /**
     */
    @Transactional(readOnly=true)
    public ExecutionStrategyTypeInfo findStrategyType(int sessionId, ExecutionStrategyType type)
        throws PermissionException, SessionException {
        sessionManager.authenticate(sessionId);
        return galertManager.findStrategyType(type);

    }

    /**
     */
    @Transactional(readOnly=true)
    public GtriggerTypeInfo findTriggerType(int sessionId, GtriggerType type)
        throws SessionException {
        sessionManager.authenticate(sessionId);
        return gtriggerManager.findTriggerType(type);
    }

    /**
     */
    public GtriggerTypeInfo registerTriggerType(int sessionId,
                                                GtriggerType type)
        throws SessionException {
        sessionManager.authenticate(sessionId);
        return gtriggerManager.registerTriggerType(type);
    }

    /**
     */
    public ExecutionStrategyInfo addPartition(int sessionId, GalertDef def, GalertDefPartition partition,
                                              ExecutionStrategyTypeInfo stratType,
                                              ConfigResponse stratConfig)
        throws SessionException {
        sessionManager.authenticate(sessionId);
        return galertManager.addPartition(def, partition, stratType, stratConfig);
    }

    /**
     */
    public GalertDef createAlertDef(int sessionId, String name,
                                    String description, AlertSeverity severity,
                                    boolean enabled, ResourceGroup group)
        throws SessionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        return galertManager.createAlertDef(subject, name, description,
                                            severity, enabled, group);
    }

    /**
     */
    public void configureTriggers(int sessionId, GalertDef def,
                                  GalertDefPartition partition,
                                  List<GtriggerTypeInfo> triggerInfos, List<ConfigResponse> configs)
        throws SessionException {
        sessionManager.authenticate(sessionId);
        galertManager.configureTriggers(def, partition, triggerInfos, configs);
    }

    /**
     * Find all the group alert definitions for a given appdef group.
     * 
     * @return a collection of {@link AlertDefinitionBean}s
     * @throws PermissionException
     */
    @Transactional(readOnly=true)
    public PageList<GalertDef> findDefinitions(int sessionId, Integer gid, PageControl pc)
        throws SessionException, PermissionException {
        AuthzSubject subj = sessionManager.getSubject(sessionId);

        // Find the ResourceGroup
        ResourceGroup g = resourceGroupManager.findResourceGroupById(subj, gid);
        PageList<GalertDef> defList = null;
        try {
            // ...check that user can view alert definitions...
            alertPermissionManager.canViewAlertDefinition(subj, AppdefUtil.newAppdefEntityId(g.getResource()));
            defList = galertManager.findAlertDefs(g, pc);
        } catch (PermissionException e) {
            // user does not have sufficient permissions, so display no
            // definitions
            defList = new PageList<GalertDef>();
        }
        return defList;
    }

    /**
     */
    public void markDefsDeleted(int sessionId, GalertDef def)
        throws SessionException {
        sessionManager.authenticate(sessionId);
        galertManager.markDefDeleted(def);
    }

    /**
     */
    public void markDefsDeleted(int sessionId, Integer[] defIds)
        throws SessionException {
        sessionManager.authenticate(sessionId);

        for (Integer defId : defIds) {
            GalertDef def = galertManager.findById(defId);
            galertManager.markDefDeleted(def);
        }
    }

    /**
     */
    @Transactional(readOnly=true)
    public GalertDef findDefinition(int sessionId, Integer id)
        throws SessionException {
        sessionManager.authenticate(sessionId);
        return galertManager.findById(id);
    }

    /**
     */
    @Transactional(readOnly=true)
    public Escalatable findEscalatableAlert(int sessionId, Integer id)
        throws SessionException, PermissionException {
        AuthzSubject subject = sessionManager.getSubject(sessionId);
        Escalatable esc = galertManager.findEscalatableAlert(id);
        Resource resource = esc.getDefinition().getDefinitionInfo().getResource();

        // HQ-1295: Does user have sufficient permissions?
        // ...check that users can view alerts...
        alertPermissionManager.canViewAlertDefinition(subject, AppdefUtil.newAppdefEntityId(resource));

        return esc;
    }

    /**
     */
    public void update(int sessionId, GalertDef def, String name, String desc,
                       AlertSeverity severity, Boolean enabled)
        throws SessionException {
        sessionManager.authenticate(sessionId);
        galertManager.update(def, name, desc, severity, enabled);
    }

    /**
     */
    public void update(int sessionId, GalertDef def, Escalation escalation)
        throws SessionException {
        sessionManager.authenticate(sessionId);
        galertManager.update(def, escalation);
    }

    /**
     * Bulk enable or disable GalertDefs
     * @throws SessionException if user session cannot be authenticated
     */
    public void enable(int sessionId, GalertDef[] defs, boolean enable)
        throws SessionException {
        sessionManager.authenticate(sessionId);
        for (GalertDef def : defs) {
            galertManager.enable(def, enable);
        }
    }

    /**
     * Count the total number of galerts in the time frame
     */
    public int countAlertLogs(int sessionId, Integer gid, long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException,
        PermissionException {

        PageList<GalertLog> alertLogs = null;

        try {
            AuthzSubject subj = sessionManager.getSubject(sessionId);
            ResourceGroup g = resourceGroupManager.findResourceGroupById(subj, gid);
            // ...check that user can view alert definitions...
            alertPermissionManager.canViewAlertDefinition(subj, AppdefUtil.newAppdefEntityId(g.getResource()));

            // Don't need to have any results
            PageControl pc = new PageControl();
            pc.setPagesize(0);

            alertLogs =
                        galertManager.findAlertLogsByTimeWindow(g, begin, end, pc);
        } catch (PermissionException e) {
            // user does not have sufficient permissions, so display no alerts
            alertLogs = new PageList<GalertLog>();
        }

        return alertLogs.getTotalSize();
    }

    /**
     * retrieve all escalation policy names as a Array of JSONObject.
     * 
     * Escalation json finders begin with json* to be consistent with
     * DAO finder convention
     * 
     */
    @Transactional(readOnly=true)
    public JSONObject findAlertLogs(int sessionId, Integer gid, long begin,
                                    long end, PageControl pc)
        throws JSONException, SessionTimeoutException, SessionNotFoundException,
        PermissionException {
        AuthzSubject subj = sessionManager.getSubject(sessionId);

        ResourceGroup g = resourceGroupManager.findResourceGroupById(subj,
                                                                     gid);
        PageList<GalertLog> alertLogs = null;
        JSONArray jarr = new JSONArray();

        try {
            AppdefEntityID entityId = AppdefUtil.newAppdefEntityId(g.getResource());
            // ...check that user can view alert definitions...
            alertPermissionManager.canViewAlertDefinition(subj, entityId);
            alertLogs =
                        galertManager.findAlertLogsByTimeWindow(g, begin, end, pc);

            for (GalertLog alert : alertLogs) {
                // Format the alertTime
                SimpleDateFormat df =
                                      new SimpleDateFormat(TimeUtil.DISPLAY_DATE_FORMAT);
                String date =
                              df.format(new Date(alert.getTimestamp()));

                long maxPauseTime = 0;
                Escalation esc = alert.getDefinition().getEscalation();
                if (esc != null && esc.isPauseAllowed()) {
                    maxPauseTime = esc.getMaxPauseTime();
                }
                
                boolean canTakeAction = false;
                try {
                    // ...check that the user can fix/acknowledge...
                    alertPermissionManager.canFixAcknowledgeAlerts(subj, entityId);
                    canTakeAction = true;
                } catch(PermissionException e) {
                    // ...the user can't fix/acknowledge...
                }

                jarr.put(new JSONObject()
                                         .put("id", alert.getId())
                                         .put("time", date)
                                         .put("name", alert.getAlertDefinitionInterface().getName())
                                         .put("defId", alert.getAlertDefinitionInterface().getId())
                                         .put("priority",
                                              alert.getAlertDefinitionInterface().getPriority())
                                         .put("reason", alert.getShortReason())
                                         .put("fixed", alert.isFixed())
                                         .put("acknowledgeable", alert.isAcknowledgeable())
                                         .put("canTakeAction", canTakeAction)
                                         .put("maxPauseTime", maxPauseTime));
            }
        } catch (PermissionException e) {
            // user does not have sufficient permissions, so display no alerts
            alertLogs = new PageList<GalertLog>();
        }

        JSONObject jobj = new JSONObject();
        jobj.put("logs", jarr);
        jobj.put("total", alertLogs.getTotalSize());

        return jobj;
    }

    /**
     * Get the last fix if available
     */
    @Transactional(readOnly=true)
    public String getLastFix(int sessionID, GalertDef def)
        throws SessionNotFoundException, SessionTimeoutException {
        return escalationManager.getLastFix(def);
    }
}
