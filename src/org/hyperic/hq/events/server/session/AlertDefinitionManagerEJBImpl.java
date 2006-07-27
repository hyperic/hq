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

package org.hyperic.hq.events.server.session;

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.shared.HQConstants;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.AlertConditionCreateException;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.ActionLocal;
import org.hyperic.hq.events.shared.ActionLocalHome;
import org.hyperic.hq.events.shared.ActionPK;
import org.hyperic.hq.events.shared.ActionUtil;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionLocal;
import org.hyperic.hq.events.shared.AlertConditionLocalHome;
import org.hyperic.hq.events.shared.AlertConditionUtil;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionBasicValue;
import org.hyperic.hq.events.shared.AlertDefinitionLocal;
import org.hyperic.hq.events.shared.AlertDefinitionLocalHome;
import org.hyperic.hq.events.shared.AlertDefinitionPK;
import org.hyperic.hq.events.shared.AlertDefinitionUtil;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerLocalHome;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerPK;
import org.hyperic.hq.events.shared.RegisteredTriggerUtil;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.util.jdbc.DBUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;

/**
 * <p> Stores Events to and deletes Events from storage
 *
 * </p>
 * @ejb:bean name="AlertDefinitionManager"
 *      jndi-name="ejb/events/AlertDefinitionManager"
 *      local-jndi-name="LocalAlertDefinitionManager"
 *      view-type="local"
 *      type="Stateless"
 * @ejb:transaction type="REQUIRED"
 *
 */
public class AlertDefinitionManagerEJBImpl extends SessionEJB
    implements SessionBean {
    private final String logCtx =
        "org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl";
    private final String VALUE_PROCESSOR =
        "org.hyperic.hq.events.server.session.PagerProcessor_events";

    private SessionContext ctx = null;
    private Pager valuePager = null;
    
    private AlertDefinitionLocalHome adHome = null;    
    private AlertDefinitionLocalHome getADHome() {
        try {
            if (adHome == null)
                adHome = AlertDefinitionUtil.getLocalHome();
            return adHome;
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    private AlertManagerLocal alertMan = null;
    private AlertManagerLocal getAlertMan() {
        try {
            if (alertMan == null)
                alertMan = AlertManagerUtil.getLocalHome().create();
            return alertMan;
        } catch (CreateException e) {
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    ActionLocalHome actHome = null;
    private ActionLocalHome getActionHome() {
        try {
            if (actHome == null)
                actHome = ActionUtil.getLocalHome();
            return actHome;
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    AlertConditionLocalHome acHome = null;
    private AlertConditionLocalHome getAcHome() {
        try {
            if (acHome == null)
                acHome = AlertConditionUtil.getLocalHome();
            return acHome;
        } catch (NamingException e) {
            throw new SystemException(e);
        }
    }

    /** Remove alert definitions
     */
    private void deleteAlertDefinition(AlertDefinitionLocal alertdef,   
                                       boolean force)
        throws RemoveException {
        
        // If this is a child alert definition, do not delete it unless forced
        if (!force && (alertdef.getParentId() != null &&
                       alertdef.getParentId().intValue() != 0))
            return;        
            
        try {
            // If there are any children, delete them, too
            List children =
                getADHome().findChildAlertDefinitions(alertdef.getId());
            for (Iterator i = children.iterator(); i.hasNext(); ) {
                AlertDefinitionLocal child =
                    (AlertDefinitionLocal) i.next();
                
                if (this.getAlertMan().getAlertCount(child.getId()) > 0)
                    child.setDeleted(true);
                else
                    deleteAlertDefinition(child, true);
            }
        } catch (FinderException e) {
            // Then we don't have to remove them :-)
        }

        // See if there are any alerts
        if (!force &&
            this.getAlertMan().getAlertCount(alertdef.getId()) > 0) {
                alertdef.setDeleted(true);
            return;
        }

        // Delete the alerts
        this.getAlertMan().deleteAlerts(alertdef.getId());

        // Actually remove the definition
        alertdef.remove();
    }

    ///////////////////////////////////////
    // operations

    /** Create a new alert definition
     * @ejb:interface-method
     */
    public AlertDefinitionValue createAlertDefinition(
        AlertDefinitionValue adval)
        throws AlertDefinitionCreateException, ActionCreateException,
               FinderException {
        ArrayList clocals = null;
        AlertConditionValue[] conds = adval.getConditions();
        if (conds.length > 0) {
            clocals = new ArrayList();
            for (int i = 0; i < conds.length; i++) {
                // Create new condition
                try {
                    clocals.add(this.getAcHome().create(conds[i]));
                } catch (CreateException e) {
                    throw new AlertConditionCreateException(conds[i], e);
                }
            }
        }
                
        // Set actions
        ArrayList alocals = null;
        ActionValue[] actions = adval.getActions();
        if (actions.length > 0) {
            alocals = new ArrayList();
            for (int i = 0; i < actions.length; i++) {
                // Create new action
                try {
                    alocals.add(this.getActionHome().create(actions[i]));
                } catch (CreateException e) {
                    throw new ActionCreateException(e);
                }
            }
        }
        
        // Set triggers
        ArrayList tlocals = null;
        RegisteredTriggerValue[] triggers = adval.getTriggers();
        if (triggers.length > 0) {
            // Set act on trigger as the last trigger
            Integer lastId = triggers[triggers.length - 1].getId();
            adval.setActOnTriggerId(lastId.intValue());
        
            RegisteredTriggerLocalHome rtHome;
            
            try {
                rtHome = RegisteredTriggerUtil.getLocalHome();
            } catch (NamingException e) {
                throw new SystemException(e);
            }
        
            tlocals = new ArrayList();
            for (int i = 0; i < triggers.length; i++) {
                // Triggers were already created by bizapp
                tlocals.add(rtHome.findByPrimaryKey(
                    new RegisteredTriggerPK(triggers[i].getId())));
            }
        }
                
        try {
            AlertDefinitionLocal alert =
                getADHome().create(adval, clocals, tlocals, alocals);
            return alert.getAlertDefinitionValue();
        } catch (CreateException e) {
            throw new AlertDefinitionCreateException(e);
        }
    }

    /**
     * Update just the basics
     *
     * @ejb:interface-method
     * @ejb:transaction-type="REQUIRED"
     */
    public void updateAlertDefinitionBasic(Integer id,
                                           String name, String desc,
                                           int priority, boolean enabled)
        throws FinderException, RemoveException {
        List alertdefs = new ArrayList();
        alertdefs.add(getADHome().findByPrimaryKey(new AlertDefinitionPK(id)));
        
        // If there are any children, add them, too
        try {
            List children = getADHome().findChildAlertDefinitions(id);
            alertdefs.addAll(children);
        } catch (FinderException e) {
            // No children is no problem
        }
        
        for (Iterator it = alertdefs.iterator(); it.hasNext(); ) {
            AlertDefinitionLocal ad = (AlertDefinitionLocal) it.next();
            ad.setName(name);
            ad.setDescription(desc);
            ad.setPriority(priority);
            ad.setEnabled(enabled);
            ad.setMtime(System.currentTimeMillis());
        }
    }

    /** Update an alert definition
     * @ejb:interface-method
     */
    public AlertDefinitionValue updateAlertDefinition(
        AlertDefinitionValue adval)
        throws AlertConditionCreateException, ActionCreateException,
               FinderException, RemoveException {

        AlertDefinitionPK pk = new AlertDefinitionPK(adval.getId());
        AlertDefinitionLocal aldef = getADHome().findByPrimaryKey(pk);
        
        // See if the conditions changed
        if (adval.getAddedConditions().size()   > 0 ||
            adval.getUpdatedConditions().size() > 0 ||
            adval.getRemovedConditions().size() > 0 ){
            // We need to keep old conditions around for the logs.  So
            // we'll create new conditions and update the alert
            // definition, but we won't remove the old conditions.
            AlertConditionValue[] conds = adval.getConditions();
            ArrayList newConds = new ArrayList(conds.length);
            for (int i = 0; i < conds.length; i++) {
                try {
                    AlertConditionLocal acLocal =
                        this.getAcHome().create(conds[i]);
                    conds[i].setId(acLocal.getId());
                    newConds.add(acLocal);
                } catch (CreateException e) {
                    throw new AlertConditionCreateException(conds[i], e);
                }
            }

            aldef.setConditions(newConds);
        }

        // See if the actions changed
        if (adval.getAddedActions().size()   > 0 ||
            adval.getUpdatedActions().size() > 0 ||
            adval.getRemovedActions().size() > 0) {
            // We need to keep old actions around for the logs.  So
            // we'll create new actions and update the alert
            // definition, but we won't remove the old conditions.
            ActionValue[] actions = adval.getActions();
            ArrayList newActions = new ArrayList(actions.length);
            for (int i = 0; i <  actions.length; i++) {
                try {
                    ActionLocal aLocal = this.getActionHome().create(actions[i]);
                    actions[i].setId(aLocal.getId());
                    newActions.add(aLocal);
                } catch (CreateException e) {
                    throw new ActionCreateException(e);
                }
            }

            aldef.setActions(newActions);
        }

        // Find out the last trigger ID (bizapp should have created them)
        RegisteredTriggerValue[] triggers = adval.getTriggers();
        if (triggers.length > 0) {
            RegisteredTriggerValue last = triggers[triggers.length - 1];
            adval.setActOnTriggerId(last.getId().intValue());
        }

        // Lastly, the modification time
        adval.setMtime(System.currentTimeMillis());

        // Now set the alertdef
        aldef.setAlertDefinitionValue(adval);
        
        return aldef.getAlertDefinitionValue();
    }

    /** Enable/Disable alert definitions
     * @ejb:interface-method
     */
    public void updateAlertDefinitionsEnable(Integer[] ids, boolean enable)
        throws FinderException {
    
        List alertdefs = new ArrayList();
        
        for (int i = 0; i < ids.length; i++) {
            AlertDefinitionPK pk = new AlertDefinitionPK(ids[i]);
            alertdefs.add(getADHome().findByPrimaryKey(pk));
            
            // If there are any children, add them, too
            try {
                List children = getADHome().findChildAlertDefinitions(ids[i]);
                alertdefs.addAll(children);
            } catch (FinderException e) {
                // No children is no problem
            }
        }

        for (Iterator it = alertdefs.iterator(); it.hasNext(); ) {
            AlertDefinitionLocal ad = (AlertDefinitionLocal) it.next();
            ad.setEnabled(enable);
            ad.setMtime(System.currentTimeMillis());
        }
    }

    /** Set the actions of an alert definition
     * @ejb:interface-method
     */
    public void updateAlertDefinitionActions(Integer aid, Integer[] acids)
        throws FinderException {
        ArrayList actions = new ArrayList();
        for (int i = 0; i < acids.length; i++) {
            ActionLocal action =
                this.getActionHome().findByPrimaryKey(new ActionPK(acids[i]));
            actions.add(action);
        }
        
        AlertDefinitionLocal ad =
            getADHome().findByPrimaryKey(new AlertDefinitionPK(aid));
            
        ad.setActions(actions);      
    }

    /** Remove alert definitions
     * @ejb:interface-method
     */
    public void deleteAlertDefinitions(Integer[] ids) throws RemoveException {
        RegisteredTriggerManagerLocal rtm;
        
        try {
            rtm = RegisteredTriggerManagerUtil.getLocalHome().create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
        
        // Get rid of their triggers first
        for (int i = 0; i < ids.length; i++) {
            try {
                rtm.deleteAlertDefinitionTriggers(ids[i]);

                List ads = getADHome().findChildAlertDefinitions(ids[i]);
                for (Iterator it = ads.iterator(); it.hasNext(); ) {
                    AlertDefinitionLocal child =
                        (AlertDefinitionLocal) it.next();
                    rtm.deleteAlertDefinitionTriggers(
                        ((AlertDefinitionPK) child.getPrimaryKey()).getId());
                }
            } catch (FinderException e) {
                // Swallow the FinderException, maybe there aren't any
            }
        }

        for (int i = 0; i < ids.length; i++) {
            AlertDefinitionPK pk = new AlertDefinitionPK(ids[i]);
            AlertDefinitionLocal alertdef;
            try {
                // Find the alert definition
                alertdef = getADHome().findByPrimaryKey(pk);
                this.deleteAlertDefinition(alertdef, false);
            } catch (FinderException e) {
                // Then we don't have to remove it :-)
            }
        }
    }

    /** Remove alert definitions
     * @ejb:interface-method
     */
    public void deleteAlertDefinitions(AppdefEntityID aeid)
        throws RemoveException {
        RegisteredTriggerManagerLocal rtm;
        
        try {
            rtm = RegisteredTriggerManagerUtil.getLocalHome().create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
        
        List adefs;
        try {
            adefs = this.getADHome().findByAppdefEntity(aeid.getType(),
                                                        aeid.getID());
        } catch (FinderException e) {
            return;
        }
        
        // Get rid of their triggers first
        for (Iterator it = adefs.iterator(); it.hasNext(); ) {
            AlertDefinitionLocal adef = (AlertDefinitionLocal) it.next();
            Integer id = ((AlertDefinitionPK) adef.getPrimaryKey()).getId();
    
            try {
                rtm.deleteAlertDefinitionTriggers(id);
                
                List ads = getADHome().findChildAlertDefinitions(id);
                for (Iterator cit = ads.iterator(); cit.hasNext();) {
                    AlertDefinitionLocal child =
                        (AlertDefinitionLocal) cit.next();
                    rtm.deleteAlertDefinitionTriggers(
                        ((AlertDefinitionPK) child.getPrimaryKey()).getId());
                }
            } catch (FinderException e) {
                // Swallow the FinderException, maybe there aren't any
            }
                
            this.deleteAlertDefinition(adef, true);
        }
    }

    /** Find an alert definition
     * Require a transaction because of the other EJBs involved in getting
     * the value object
     * @ejb:interface-method
     */
    public AlertDefinitionValue getById(Integer id)
        throws FinderException {
        AlertDefinitionLocal ad =
            getADHome().findByPrimaryKey(new AlertDefinitionPK(id));
        return ad.getAlertDefinitionValue();
    }
    
    /** Find an alert definition
     * Require a transaction because of the other EJBs involved in getting
     * the value object
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public AlertDefinitionBasicValue getBasicById(Integer id)
        throws FinderException {
        AlertDefinitionLocal ad =
            getADHome().findByPrimaryKey(new AlertDefinitionPK(id));
        return ad.getAlertDefinitionBasicValue();
    }
    
    /** Decide if a trigger should fire the alert, use direct SQL to avoid JBOSS
     * caching the EJB in HA situations.  Plus we can minimize the number of
     * columns looked up if the alert definition is not actually active
     * @ejb:interface-method
     * @param tid the trigger ID
     * @return the ID of the alert definition
     */
    public Integer getIdFromTrigger(Integer tid) {
        Connection        conn = null;
        PreparedStatement stmt = null;
        ResultSet         rs   = null;

        try {
            conn =
                DBUtil.getConnByContext(getInitialContext(),
                                        HQConstants.DATASOURCE);
    
            stmt = conn.prepareStatement(
                "SELECT id FROM EAM_ALERT_DEFINITION " +
                " WHERE act_on_trigger_id = ? AND enabled = ? AND deleted = ?");
    
            int i = 1;
            stmt.setInt (i++, tid.intValue());
            stmt.setBoolean(i++, true);
            stmt.setBoolean(i++, false);
            rs = stmt.executeQuery();

            if(rs.next())
                return new Integer(rs.getInt(1));
        } catch (SQLException e) {
            log.error("SQLException determining if alert definition is enabled",
                      e);
            throw new SystemException(e);
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }
        
        return null;
    }
    
    /** Get an alert definition's appdef entity ID
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public AppdefEntityID getAppdefEntityIdById(Integer id)
        throws FinderException {
        AlertDefinitionLocal ad =
            getADHome().findByPrimaryKey(new AlertDefinitionPK(id));
        return new AppdefEntityID(ad.getAppdefType(), ad.getAppdefId());
    }
    
    /** Get an alert definition's name
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public String getNameById(Integer id)
        throws FinderException {
        AlertDefinitionLocal ad =
            getADHome().findByPrimaryKey(new AlertDefinitionPK(id));
        return ad.getName();
    }

    /** Get an alert definition's conditions
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public AlertConditionValue[] getConditionsById(Integer id)
        throws FinderException {
        AlertDefinitionLocal ad =
            getADHome().findByPrimaryKey(new AlertDefinitionPK(id));
        
        Collection conds = ad.getConditions();
        AlertConditionValue[] condVals = new AlertConditionValue[conds.size()];
        Iterator it = conds.iterator();
        for (int i = 0; it.hasNext(); i++) {
            AlertConditionLocal cond = (AlertConditionLocal) it.next();
            condVals[i] = cond.getAlertConditionValue();
        }
        return condVals;
    }
    
    /** Get an alert definition's actions
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public ActionValue[] getActionsById(Integer id)
        throws FinderException {
        AlertDefinitionLocal ad =
            getADHome().findByPrimaryKey(new AlertDefinitionPK(id));
        
        Collection acts = ad.getActions();
        ActionValue[] actVals = new ActionValue[acts.size()];
        Iterator it = acts.iterator();
        for (int i = 0; it.hasNext(); i++) {
            ActionLocal cond = (ActionLocal) it.next();
            actVals[i] = cond.getActionValue();
        }
        return actVals;
    }
    
    /** Get list of alert conditions for a resource or resource type
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public boolean isAlertDefined(AppdefEntityID id, Integer parentId) {
        try {
            List adefs = this.getADHome().findEntityChildAlertDefinitions(
                    id.getType(), id.getID(), parentId);
            return adefs.size() > 0;
        } catch (FinderException e) {
            // No alert definitions found
            return false;
        }
    }

    /** Get list of all alert conditions
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public PageList findAllAlertDefinitions() {
        ArrayList vals = new ArrayList();
        try {
            List ads = getADHome().findAll();
            for (Iterator i = ads.iterator(); i.hasNext(); ) {
                AlertDefinitionLocal alert = (AlertDefinitionLocal) i.next();
                vals.add(alert.getAlertDefinitionValue());
            }
        } catch (FinderException e) {
            // No triggers found, just return an empty list, then
        }
        return new PageList(vals, vals.size());
    }

    /** Get list of all child conditions
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public PageList findChildAlertDefinitions(Integer id) {
        ArrayList vals = new ArrayList();
        try {
            List ads = getADHome().findChildAlertDefinitions(id);
            for (Iterator i = ads.iterator(); i.hasNext(); ) {
                AlertDefinitionLocal alert = (AlertDefinitionLocal) i.next();
                vals.add(alert.getAlertDefinitionValue());
            }
        } catch (FinderException e) {
            // No triggers found, just return an empty list, then
        }
        return new PageList(vals, vals.size());
    }

    /** Get the resource-specific alert definition ID by parent ID
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public Integer findChildAlertDefinitionId(AppdefEntityID aeid,
                                              Integer pid)
        throws FinderException {
        AlertDefinitionLocal ad =
            getADHome().findEntityChildAlertDefinition(aeid.getType(),
                                                       aeid.getID(), pid);
        return ad.getId();
    }

    /** Get list of alert conditions for a resource
     * Require a transaction because of the other EJBs involved in getting
     * the value object
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(AppdefEntityID id, PageControl pc) {
        try {
            List adefs;
            if (pc.getSortattribute() == SortAttribute.CTIME) {
                adefs =
                    this.getADHome().findByAppdefEntitySortByCtime(
                        id.getType(), id.getID());
            }
            else {
                adefs =
                    this.getADHome().findByAppdefEntity(id.getType(),
                                                        id.getID());
            }
                
            if (pc.getSortorder() == PageControl.SORT_DESC)
                Collections.reverse(adefs);

            return valuePager.seek(adefs, pc.getPagenum(), pc.getPagesize());
        } catch (FinderException e) {
            // No triggers found, just return an empty list, then
            return new PageList();
        }
    }

    /** Get list of alert conditions for a resource or resource type
     * Require a transaction because of the other EJBs involved in getting
     * the value object
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(AppdefEntityID id, Integer parentId,
                                         PageControl pc) {
        try {
            List adefs;
            if (parentId == EventConstants.TYPE_ALERT_DEF_ID) {
                adefs =
                    this.getADHome().findByAppdefEntityType(
                        id.getType(), id.getID());
            }
            else {
                adefs =
                    this.getADHome().findEntityChildAlertDefinitions(
                            id.getType(), id.getID(), parentId);
            }
                
            if (pc.getSortorder() == PageControl.SORT_DESC)
                Collections.reverse(adefs);

            return valuePager.seek(adefs, pc.getPagenum(), pc.getPagesize());
        } catch (FinderException e) {
            // No triggers found, just return an empty list, then
            return new PageList();
        }
    }

    /** Get list of children alert definition for a parent alert definition
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public PageList findAlertDefinitionChildren(Integer id) {
        try {
            List adefs = getADHome().findChildAlertDefinitions(id);
            PageControl pc = PageControl.PAGE_ALL;
            return valuePager.seek(adefs, pc.getPagenum(), pc.getPagesize());
        } catch (FinderException e) {
            // No children found, just return an empty list, then
            return new PageList();
        }
    }

    /** Get list of children alert definition IDs for a parent alert definition
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public List findAlertDefinitionChildrenIds(Integer id) {
        ArrayList ids = new ArrayList();
        try {
            List adefs = getADHome().findChildAlertDefinitions(id);
            for (Iterator it = adefs.iterator(); it.hasNext(); ) {
                AlertDefinitionLocal loc = (AlertDefinitionLocal) it.next();
                ids.add(((AlertDefinitionPK) loc.getPrimaryKey()).getId());
            }
        } catch (FinderException e) {
            // No children found, just return an empty list, then
        }
        
        return ids;
    }

    /** Get list of alert definition names for a resource
     * @ejb:interface-method
     * @ejb:transaction type="SUPPORTS"
     */
    public SortedMap findAlertDefinitionNames(AppdefEntityID id,
                                              Integer parentId) {
        TreeMap ret = new TreeMap();
        try {
            List adefs;
            
            if (parentId != null &&
                EventConstants.TYPE_ALERT_DEF_ID.equals(parentId)) {
                adefs = getADHome().findEntityChildAlertDefinitions(
                    id.getType(), id.getID(), parentId);
            }
            else {
                adefs = getADHome().findByAppdefEntity(
                    id.getType(), id.getID());
            }
            
            // Use name as key so that map is sorted
            for (Iterator it = adefs.iterator(); it.hasNext(); ) {
                AlertDefinitionLocal adLocal = (AlertDefinitionLocal) it.next();
                ret.put(adLocal.getName(), adLocal.getId());
            }
        } catch (FinderException e) {
            // No definitions found, just return an empty list, then
        }
        return ret;
    }

    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.SessionBean#ejbCreate()
     * @ejb:create-method
     */
    public void ejbCreate() throws CreateException {
        try {
            valuePager = Pager.getPager(VALUE_PROCESSOR);
        } catch ( Exception e ) {
            throw new CreateException("Could not create value pager:" + e);
        }
    }

    /**
     * @see javax.ejb.SessionBean#ejbPostCreate()
     */
    public void ejbPostCreate() {}

    /**
     * @see javax.ejb.SessionBean#ejbActivate()
     */
    public void ejbActivate() {}

    /**
     * @see javax.ejb.SessionBean#ejbPassivate()
     */
    public void ejbPassivate() {}

    /**
     * @see javax.ejb.SessionBean#ejbRemove()
     */
    public void ejbRemove() {
        this.ctx = null;
    }

    /**
     * @see javax.ejb.SessionBean#setSessionContext(SessionContext)
     */
    public void setSessionContext(SessionContext ctx)
        throws EJBException, RemoteException {
        this.ctx = ctx;
    }

}   // end AlertDefinitionManagerEJB



