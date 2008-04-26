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

package org.hyperic.hq.bizapp.server.session;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.ejb.FinderException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyInfo;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyType;
import org.hyperic.hq.galerts.server.session.ExecutionStrategyTypeInfo;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertDefPartition;
import org.hyperic.hq.galerts.server.session.GalertLog;
import org.hyperic.hq.galerts.server.session.GalertManagerEJBImpl;
import org.hyperic.hq.galerts.server.session.GtriggerManagerEJBImpl;
import org.hyperic.hq.galerts.server.session.GtriggerType;
import org.hyperic.hq.galerts.server.session.GtriggerTypeInfo;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
import org.hyperic.hq.galerts.shared.GtriggerManagerLocal;
import org.hyperic.util.TimeUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/** 
* The BizApp's interface to the Events Subsystem
*
* @ejb:bean name="GalertBoss"
*      jndi-name="ejb/bizapp/GalertBoss"
*      local-jndi-name="LocalGalertBoss"
*      view-type="both"
*      type="Stateless"
* 
* @ejb:transaction type="REQUIRED"
*/
public class GalertBossEJBImpl 
   implements SessionBean 
{
    private final Log _log = LogFactory.getLog(GalertBossEJBImpl.class); 

    private SessionManager          _sessMan;
    private GalertManagerLocal      _galertMan;
    private GtriggerManagerLocal    _triggerMan;

    public GalertBossEJBImpl() {
        _sessMan = SessionManager.getInstance();
        try {
            _galertMan  = GalertManagerEJBImpl.getOne();
            _triggerMan = GtriggerManagerEJBImpl.getOne();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }
    
    /**
     * @ejb:interface-method
     */
    public ExecutionStrategyTypeInfo 
        registerExecutionStrategy(int sessionId, 
                                  ExecutionStrategyType stratType)
        throws PermissionException, SessionException
    {
        _sessMan.authenticate(sessionId);
        return _galertMan.registerExecutionStrategy(stratType);
                          
    }

    /**
     * @ejb:interface-method
     */
    public ExecutionStrategyTypeInfo 
        findStrategyType(int sessionId, ExecutionStrategyType type)
        throws PermissionException, SessionException
    {
        _sessMan.authenticate(sessionId);
        return _galertMan.findStrategyType(type);
                          
    }
    
    /**
     * @ejb:interface-method
     */
    public GtriggerTypeInfo findTriggerType(int sessionId, GtriggerType type) 
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        return _triggerMan.findTriggerType(type);
    }
        
    /**
     * @ejb:interface-method
     */
    public GtriggerTypeInfo registerTriggerType(int sessionId, 
                                                GtriggerType type) 
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        return _triggerMan.registerTriggerType(type);
    }
        

    /**
     * @ejb:interface-method
     */
    public ExecutionStrategyInfo 
        addPartition(int sessionId, GalertDef def, GalertDefPartition partition, 
                     ExecutionStrategyTypeInfo stratType, 
                     ConfigResponse stratConfig)
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        return _galertMan.addPartition(def, partition, stratType, stratConfig);
    }
    

    /**
     * @ejb:interface-method
     */
    public GalertDef createAlertDef(int sessionId, String name,
                                    String description, AlertSeverity severity,
                                    boolean enabled, ResourceGroup group) 
        throws SessionException
    {
        AuthzSubject subject = _sessMan.getSubjectPojo(sessionId);

        return _galertMan.createAlertDef(subject, name, description,
                                         severity, enabled, group);
    }

    /**
     * @ejb:interface-method
     */
    public void configureTriggers(int sessionId, GalertDef def, 
                                  GalertDefPartition partition,
                                  List triggerInfos, List configs)
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        _galertMan.configureTriggers(def, partition, triggerInfos, configs);
    }

    /**
     * Find all the group alert definitions for a given appdef group.
     * 
     * @return a collection of {@link AlertDefinitionBean}s
     * @throws PermissionException 
     * @ejb:interface-method
     */
    public PageList findDefinitions(int sessionId, Integer gid, PageControl pc)
        throws SessionException, PermissionException
    {
        AuthzSubject subj = _sessMan.getSubjectPojo(sessionId);
        
        // Find the ResourceGroup
        ResourceGroup g;
        g = ResourceGroupManagerEJBImpl.getOne().findResourceGroupById(subj, 
                                                                       gid);
        return _galertMan.findAlertDefs(g, pc);
    }

    /**
     * @ejb:interface-method
     */
    public void markDefsDeleted(int sessionId, GalertDef def) 
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        _galertMan.markDefDeleted(def);
    }    

    /**
     * @ejb:interface-method
     */
    public void markDefsDeleted(int sessionId, Integer[] defIds) 
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        
        for (int i = 0; i < defIds.length; i++) {
            GalertDef def = _galertMan.findById(defIds[i]);
            _galertMan.markDefDeleted(def);
        }
    }    

    /**
     * @ejb:interface-method
     */
    public GalertDef findDefinition(int sessionId, Integer id)
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        return _galertMan.findById(id);
    }    

    /**
     * @ejb:interface-method
     */
    public Escalatable findEscalatableAlert(int sessionId, Integer id)
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        return _galertMan.findEscalatableAlert(id);
    }    

    /**
     * @ejb:interface-method  
     */
    public void update(int sessionId, GalertDef def, String name, String desc, 
                       AlertSeverity severity, Boolean enabled)
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        _galertMan.update(def, name, desc, severity, enabled);
    }
    
    /**
     * @ejb:interface-method  
     */
    public void update(int sessionId, GalertDef def, Escalation escalation) 
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        _galertMan.update(def, escalation);
    }
    
    /**
     * Bulk enable or disable GalertDefs
     * @throws SessionException if user session cannot be authenticated
     * @ejb:interface-method
     */
    public void enable(int sessionId, GalertDef[] defs, boolean enable)
        throws SessionException
    {
        _sessMan.authenticate(sessionId);
        for (int i = 0; i < defs.length; i++) {
            _galertMan.enable(defs[i], enable);
        }
    }

    /**
     * Count the total number of galerts in the time frame
     * @ejb:interface-method
     */
    public int countAlertLogs(int sessionId, Integer gid, long begin, long end)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        AuthzSubject subj = _sessMan.getSubjectPojo(sessionId);

        ResourceGroup g;
        g = ResourceGroupManagerEJBImpl.getOne().findResourceGroupById(subj,
                                                                       gid);
        
        // Don't need to have any results
        PageControl pc = new PageControl();
        pc.setPagesize(0);
        
        PageList alertLogs =
            _galertMan.findAlertLogsByTimeWindow(g, begin, end, pc);
        
        return alertLogs.getTotalSize();
    }
    
    /**
     * retrieve all escalation policy names as a Array of JSONObject.
     *
     * Escalation json finders begin with json* to be consistent with
     * DAO finder convention
     *
     * @ejb:interface-method
     */
    public JSONObject findAlertLogs(int sessionId, Integer gid, long begin,
                                    long end, PageControl pc)
        throws JSONException, SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        AuthzSubject subj = _sessMan.getSubjectPojo(sessionId);

        ResourceGroup g;
        g = ResourceGroupManagerEJBImpl.getOne().findResourceGroupById(subj,
                                                                       gid);
        
        PageList alertLogs =
            _galertMan.findAlertLogsByTimeWindow(g, begin, end, pc);
        
        JSONArray jarr = new JSONArray();
        for (Iterator i = alertLogs.iterator(); i.hasNext(); ) {
            GalertLog alert = (GalertLog) i.next();
            
            // Format the alertTime
            SimpleDateFormat df =
                new SimpleDateFormat(TimeUtil.DISPLAY_DATE_FORMAT);
            String date =
                df.format(new Date(alert.getTimestamp()));
            
            jarr.put(new JSONObject()
                .put("id", alert.getId())
                .put("time", date)
                .put("name", alert.getAlertDefinitionInterface().getName())
                .put("defId", alert.getAlertDefinitionInterface().getId())
                .put("priority",
                     alert.getAlertDefinitionInterface().getPriority())
                .put("reason", alert.getShortReason())
                .put("fixed", alert.isFixed())
                .put("acknowledgeable", alert.isAcknowledgeable()));
        }
        
        JSONObject jobj = new JSONObject();
        jobj.put("logs", jarr);
        jobj.put("total", alertLogs.getTotalSize());
        
        return jobj;
    }
    
    /** Get the last fix if available
     * @ejb:interface-method
     */
    public String getLastFix(int sessionID, GalertDef def)
        throws SessionNotFoundException, SessionTimeoutException {        
        // Look for the last fixed alert
        return EscalationManagerEJBImpl.getOne().getLastFix(def);
    }
    
    /**
     * @ejb:create-method
     */
    public void ejbCreate() {}
    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
