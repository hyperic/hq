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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.ejb.CreateException;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.hibernate.ObjectNotFoundException;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.dao.ActionDAO;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.AlertConditionCreateException;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.EventConstants;
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
import org.hyperic.hq.events.server.session.AlertDefinition;
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
public class AlertDefinitionManagerEJBImpl 
    extends SessionEJB
    implements SessionBean 
{
    private final String VALUE_PROCESSOR =
        PagerProcessor_events.class.getName();

    private Pager _valuePager;
    
    private AlertDefinitionDAO getAlertDefDAO() {
        return DAOFactory.getDAOFactory().getAlertDefDAO();
    }
    
    private ActionDAO getActionDAO() {
        return DAOFactory.getDAOFactory().getActionDAO();
    }

    private TriggerDAO getTriggerDAO() {
        return DAOFactory.getDAOFactory().getTriggerDAO();
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

    /** 
     * Remove alert definitions
     */
    private void deleteAlertDefinition(AlertDefinition alertdef, boolean force)
        throws RemoveException 
    {
        // If this is a child alert definition, do not delete it unless forced
        if (!force && alertdef.isResourceTypeDefinition())
            return;        
            
        // If there are any children, delete them, too
        for (Iterator i=alertdef.getChildren().iterator(); i.hasNext(); ) { 
            AlertDefinition child = (AlertDefinition) i.next();
            
            if (getAlertMan().getAlertCount(child.getId()) > 0)
                child.setDeleted(true);
            else
                deleteAlertDefinition(child, true);
        }

        // See if there are any alerts
        if (!force && getAlertMan().getAlertCount(alertdef.getId()) > 0) {
            alertdef.setDeleted(true);
            return;
        }

        // Delete the alerts
        getAlertMan().deleteAlerts(alertdef.getId());

        // Actually remove the definition
        getAlertDefDAO().remove(alertdef);
    }

    ///////////////////////////////////////
    // operations

    /** 
     * Create a new alert definition
     * 
     * @ejb:interface-method
     */
    public AlertDefinitionValue createAlertDefinition(AlertDefinitionValue a)
        throws AlertDefinitionCreateException, ActionCreateException,
               FinderException 
    {
        AlertDefinition res = new AlertDefinition();
        TriggerDAO tDAO = getTriggerDAO();
        ActionDAO aDAO = getActionDAO();
        AlertDefinitionDAO adDAO = getAlertDefDAO();
        
        // The following is duplicated out of what the EJBImpl did.  Makes sense
        a.cleanAction();
        a.cleanCondition();
        a.cleanTrigger();
        res.setAlertDefinitionValue(a);
        
        // Create new conditions
        AlertConditionValue[] conds = a.getConditions();
        for (int i = 0; i < conds.length; i++) {
            RegisteredTrigger trigger = tDAO.findById(conds[i].getTriggerId());

            res.createCondition(conds[i], trigger);
        }
                
        // Create actions
        ActionValue[] actions = a.getActions();
        for (int i = 0; i < actions.length; i++) {
            Action parent = null;
            
            if (actions[i].getParentId() != null)
                parent = aDAO.findById(actions[i].getParentId());
            
            res.createAction(actions[i], parent);
        }
        
        // Set triggers
        RegisteredTriggerValue[] triggers = a.getTriggers();
        if (triggers.length != 0) {
            // Set act on trigger as the last trigger
            Integer lastId = triggers[triggers.length - 1].getId();
            RegisteredTrigger actOnTrigger = tDAO.findById(lastId);
            res.setActOnTrigger(actOnTrigger);
        
            for (int i = 0; i < triggers.length; i++) {
                RegisteredTrigger trig;
            
                // Triggers were already created by bizapp, so we only need
                // to add them to our list
                trig = tDAO.findById(triggers[i].getId());
                res.addTrigger(trig);
            }
        }

        // Alert definitions are the root of the cascade relationship, so
        // we must explicitly save them
        adDAO.save(res);
        return res.getAlertDefinitionValue();
    }

    /**
     * Update just the basics
     *
     * @ejb:interface-method
     */
    public void updateAlertDefinitionBasic(Integer id, String name, String desc,
                                           int priority, boolean enabled)
    {
        List alertdefs = new ArrayList(1);
        AlertDefinition def = getAlertDefDAO().findById(id);
        
        alertdefs.add(def);
        
        // If there are any children, add them, too
        alertdefs.addAll(def.getChildren());
        
        for (Iterator i = alertdefs.iterator(); i.hasNext(); ) {
            AlertDefinition ad = (AlertDefinition)i.next();

            ad.setName(name);
            ad.setDescription(desc);
            ad.setPriority(priority);
            ad.setEnabled(enabled);
            ad.setMtime(System.currentTimeMillis());
        }
    }

    /** 
     * Update an alert definition
     * @ejb:interface-method
     */
    public AlertDefinitionValue updateAlertDefinition(AlertDefinitionValue adval)
        throws AlertConditionCreateException, ActionCreateException,
               FinderException, RemoveException 
    {
        AlertDefinition aldef = getAlertDefDAO().findById(adval.getId());
        
        // See if the conditions changed
        if (adval.getAddedConditions().size()   > 0 ||
            adval.getUpdatedConditions().size() > 0 ||
            adval.getRemovedConditions().size() > 0 )
        {
            // We need to keep old conditions around for the logs.  So
            // we'll create new conditions and update the alert
            // definition, but we won't remove the old conditions.
            AlertConditionValue[] conds = adval.getConditions();

            aldef.clearConditions();
            for (int i = 0; i < conds.length; i++) {
                RegisteredTrigger trigger = 
                    getTriggerDAO().findById(conds[i].getTriggerId());
                    
                aldef.createCondition(conds[i], trigger);
            }
        }

        // See if the actions changed
        if (adval.getAddedActions().size()   > 0 ||
            adval.getUpdatedActions().size() > 0 ||
            adval.getRemovedActions().size() > 0) 
        {
            // We need to keep old actions around for the logs.  So
            // we'll create new actions and update the alert
            // definition, but we won't remove the old conditions.
            ActionValue[] actions = adval.getActions();

            aldef.clearActions();
            for (int i = 0; i <  actions.length; i++) {
                Action parent = null;
                
                if (actions[i].getParentId() != null)
                    parent = getActionDAO().findById(actions[i].getParentId());
                aldef.createAction(actions[i], parent);
            }
        }

        // Find out the last trigger ID (bizapp should have created them)
        RegisteredTriggerValue[] triggers = adval.getTriggers();
        if (triggers.length > 0) {
            RegisteredTriggerValue last = triggers[triggers.length - 1];
            RegisteredTrigger t = getTriggerDAO().findById(last.getId());

            aldef.setActOnTrigger(t);
        }

        // Lastly, the modification time
        adval.setMtime(System.currentTimeMillis());

        // Now set the alertdef
        aldef.setAlertDefinitionValueNoRels(adval);
        
        return aldef.getAlertDefinitionValue();
    }

    /** 
     * Enable/Disable alert definitions
     * @ejb:interface-method
     */
    public void updateAlertDefinitionsEnable(Integer[] ids, boolean enable)
        throws FinderException 
    {
        AlertDefinitionDAO aDAO = getAlertDefDAO();
        List alertdefs = new ArrayList();
        
        for (int i = 0; i < ids.length; i++) {
            AlertDefinition alert = aDAO.findById(ids[i]);

            alertdefs.add(alert);
            
            // If there are any children, add them, too
            alertdefs.addAll(alert.getChildren());
        }

        for (Iterator i = alertdefs.iterator(); i.hasNext(); ) {
            AlertDefinitionLocal ad = (AlertDefinitionLocal) i.next();

            ad.setEnabled(enable);
            ad.setMtime(System.currentTimeMillis());
        }
    }

    /** 
     * Add an action to an alert definition
     * 
     * @ejb:interface-method
     */
    public void addAction(AlertDefinition def, ActionValue action)
        throws FinderException 
    {
        Action parent = null;
        
        if (action.getParentId() != null)
            parent = getActionDAO().findById(action.getParentId());
            
        def.createAction(action, parent);
    }

    /** 
     * Remove alert definitions
     *
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
            AlertDefinition def = getAlertDefDAO().findById(ids[i]);

            rtm.deleteAlertDefinitionTriggers(ids[i]);

            for (Iterator it = def.getChildren().iterator(); it.hasNext(); ) {
                AlertDefinition child = (AlertDefinition)it.next();

                rtm.deleteAlertDefinitionTriggers(child.getId());
            }
        }

        for (int i = 0; i < ids.length; i++) {
            AlertDefinition alertdef = getAlertDefDAO().findById(ids[i]);

            deleteAlertDefinition(alertdef, false);
        }
    }

    /** Remove alert definitions
     * @ejb:interface-method
     */
    public void deleteAlertDefinitions(AppdefEntityID aeid)
        throws RemoveException 
    {
        RegisteredTriggerManagerLocal rtm;
        AlertDefinitionDAO aDao = getAlertDefDAO();
        
        try {
            rtm = RegisteredTriggerManagerUtil.getLocalHome().create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
        
        List adefs = aDao.findByAppdefEntity(aeid.getType(), aeid.getID());
        
        // Get rid of their triggers first
        for (Iterator i = adefs.iterator(); i.hasNext(); ) {
            AlertDefinition adef = (AlertDefinition) i.next();
    
            rtm.deleteAlertDefinitionTriggers(adef.getId());
                
            Collection ads = adef.getChildren();
            for (Iterator cit = ads.iterator(); cit.hasNext();) {
                AlertDefinition child = (AlertDefinition) cit.next();
                        
                rtm.deleteAlertDefinitionTriggers(child.getId());
            }
                
            deleteAlertDefinition(adef, true);
        }
    }

    private AlertDefinition badFindById(Integer id) 
        throws FinderException
    {
        try {
            return getAlertDefDAO().findById(id);
        } catch(ObjectNotFoundException e) {
            throw new FinderException("Couldn't find AlertDefinition#" + id);
        }
    }
    
    /** Find an alert definition
     * @ejb:interface-method
     */
    public AlertDefinitionValue getById(Integer id) 
        throws FinderException
    {
        return badFindById(id).getAlertDefinitionValue();
    }
    
    /** Find an alert definition
     * @ejb:interface-method
     */
    public AlertDefinitionBasicValue getBasicById(Integer id) 
        throws FinderException
    {
        return badFindById(id).getAlertDefinitionBasicValue();
    }
    
    /** 
     * Decide if a trigger should fire the alert
     *
     * @ejb:interface-method
     * @param tid the trigger ID
     * @return the ID of the alert definition
     */
    public Integer getIdFromTrigger(Integer tid) {
        RegisteredTrigger trigger = getTriggerDAO().findById(tid);
        AlertDefinition def = getAlertDefDAO().getFromTrigger(trigger);
        
        if (def == null)
            return null;
        else
            return def.getId();
    }
    
    /** 
     * Get an alert definition's appdef entity ID
     * @ejb:interface-method
     */
    public AppdefEntityID getAppdefEntityIdById(Integer id)
        throws FinderException
    {
        return badFindById(id).getAppdefEntityId();
    }
    
    /** Get an alert definition's name
     * @ejb:interface-method
     */
    public String getNameById(Integer id)
        throws FinderException 
    {
        return badFindById(id).getName();
    }

    /** Get an alert definition's conditions
     * @ejb:interface-method
     */
    public AlertConditionValue[] getConditionsById(Integer id)
        throws FinderException 
    {
        AlertDefinition def = badFindById(id);
        Collection conds = def.getConditions();
        AlertConditionValue[] condVals = new AlertConditionValue[conds.size()];

        Iterator it = conds.iterator();
        for (int i = 0; it.hasNext(); i++) {
            AlertCondition cond = (AlertCondition) it.next();
            condVals[i] = cond.getAlertConditionValue();
        }
        return condVals;
    }
    
    /** Get an alert definition's actions
     * @ejb:interface-method
     */
    public ActionValue[] getActionsById(Integer id)
        throws FinderException 
    {
        AlertDefinition def = badFindById(id);
        
        Collection acts = def.getActions();
        ActionValue[] actVals = new ActionValue[acts.size()];
        Iterator it = acts.iterator();
        for (int i = 0; it.hasNext(); i++) {
            Action action = (Action) it.next();
            actVals[i] = action.getActionValue();
        }
        return actVals;
    }
    
    /** Get list of alert conditions for a resource or resource type
     * @ejb:interface-method
     */
    public boolean isAlertDefined(AppdefEntityID id, Integer parentId) {
        return getAlertDefDAO().findChildAlertDefs(id, parentId).size() > 0;
    }

    /** 
     * Get list of all alert conditions
     * 
     * @return a PageList of {@link AlertDefinitionValue} objects
     * @ejb:interface-method
     */
    public PageList findAllAlertDefinitions() {
        List vals = new ArrayList();
        
        for (Iterator i = getAlertDefDAO().findAll().iterator(); 
             i.hasNext(); ) 
        {
            AlertDefinition a = (AlertDefinition)i.next();
        
            vals.add(a.getAlertDefinitionValue());
        }
        return new PageList(vals, vals.size());
    }

    /** Get list of all child conditions
     * @ejb:interface-method
     */
    public PageList findChildAlertDefinitions(Integer id) {
        AlertDefinition def = getAlertDefDAO().findById(id);
        List vals = new ArrayList();

        List ads = getAlertDefDAO().findChildAlertDefinitions(def);
        for (Iterator i=ads.iterator(); i.hasNext(); ) {
            AlertDefinition child = (AlertDefinition)i.next();

            vals.add(child.getAlertDefinitionValue());
        }

        return new PageList(vals, vals.size());
    }

    /** Get the resource-specific alert definition ID by parent ID
     * @ejb:interface-method
     */
    public Integer findChildAlertDefinitionId(AppdefEntityID aeid,
                                              Integer pid)
        throws FinderException 
    {
        Collection res = getAlertDefDAO().findChildAlertDefs(aeid, pid);

        if (res.isEmpty())
            return null;
        return ((AlertDefinition)res.iterator().next()).getId();
    }

    /** 
     * Get list of alert conditions for a resource
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(AppdefEntityID id, PageControl pc) {
        AlertDefinitionDAO aDao = getAlertDefDAO(); 
        
        List adefs;
        if (pc.getSortattribute() == SortAttribute.CTIME) {
            adefs = aDao.findByAppdefEntitySortByCtime(id.getType(),
                                                       id.getID());
        } else {
            adefs = aDao.findByAppdefEntity(id.getType(), id.getID());
        }
                
        if (pc.getSortorder() == PageControl.SORT_DESC)
            Collections.reverse(adefs);

        return _valuePager.seek(adefs, pc.getPagenum(), pc.getPagesize());
    }

    /** 
     * Get list of alert conditions for a resource or resource type
     *
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(AppdefEntityID id, Integer parentId,
                                         PageControl pc) 
    {
        AlertDefinitionDAO aDao = getAlertDefDAO();

        List adefs;
        if (parentId == EventConstants.TYPE_ALERT_DEF_ID) {
            adefs = aDao.findByAppdefEntityType(id);
        } else {
            adefs = aDao.findChildAlertDefs(id, parentId);
        }
                
        if (pc.getSortorder() == PageControl.SORT_DESC)
            Collections.reverse(adefs);

        return _valuePager.seek(adefs, pc.getPagenum(), pc.getPagesize());
    }

    /** 
     * Get list of children alert definition for a parent alert definition
     * @ejb:interface-method
     */
    public PageList findAlertDefinitionChildren(Integer id) {
        AlertDefinition def = getAlertDefDAO().findById(id);
        
        PageControl pc = PageControl.PAGE_ALL;
        return _valuePager.seek(def.getChildren(), pc.getPagenum(), 
                               pc.getPagesize());
    }

    /** Get list of children alert definition IDs for a parent alert definition
     * @ejb:interface-method
     */
    public List findAlertDefinitionChildrenIds(Integer id) {
        AlertDefinition def = getAlertDefDAO().findById(id);
        List ids = new ArrayList(def.getChildren().size());
        
        for (Iterator i = def.getChildren().iterator(); i.hasNext(); ) {
            AlertDefinition child = (AlertDefinition) i.next();

            ids.add(child.getId());    
        }
        
        return ids;
    }

    /** 
     * Get list of alert definition names for a resource
     * @ejb:interface-method
     */
    public SortedMap findAlertDefinitionNames(AppdefEntityID id,
                                              Integer parentId) 
    {
        AlertDefinitionDAO aDao = getAlertDefDAO();
        TreeMap ret = new TreeMap();
        List adefs;
            
        if (parentId != null &&
            EventConstants.TYPE_ALERT_DEF_ID.equals(parentId)) 
        {
            adefs = aDao.findChildAlertDefs(id, parentId);
        } else {
            adefs = aDao.findByAppdefEntity(id.getType(), id.getID());
        }
            
        // Use name as key so that map is sorted
        for (Iterator i = adefs.iterator(); i.hasNext(); ) {
            AlertDefinition adLocal = (AlertDefinition) i.next();

            ret.put(adLocal.getName(), adLocal.getId());
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
            _valuePager = Pager.getPager(VALUE_PROCESSOR);
        } catch ( Exception e ) {
            throw new CreateException("Could not create value pager:" + e);
        }
    }

    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}
}



