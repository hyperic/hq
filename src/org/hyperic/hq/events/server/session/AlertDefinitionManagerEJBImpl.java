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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ObjectNotFoundException;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.AlertConditionCreateException;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionManagerUtil;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.timer.StopWatch;

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
    extends SessionBase
    implements SessionBean 
{
    private Log log = LogFactory.getLog(AlertDefinitionManagerEJBImpl.class);
    
    private final String VALUE_PROCESSOR =
        PagerProcessor_events.class.getName();

    private Pager _valuePager;
    
    private AlertDefinitionDAO getAlertDefDAO() {
        return new AlertDefinitionDAO(DAOFactory.getDAOFactory());
    }
    
    private ActionDAO getActionDAO() {
        return new ActionDAO(DAOFactory.getDAOFactory());
    }

    private TriggerDAO getTriggerDAO() {
        return new TriggerDAO(DAOFactory.getDAOFactory());
    }

    private AlertDAO getAlertDAO() {
        return new AlertDAO(DAOFactory.getDAOFactory());
    }

    private AlertConditionDAO getConditionDAO() {
        return new AlertConditionDAO(DAOFactory.getDAOFactory());
    }

    /** 
     * Remove alert definitions. It is assumed that the subject has permission 
     * to remove this alert definition and any of its' child alert definitions.
     */
    private boolean deleteAlertDefinition(AuthzSubjectValue subj,
                                          AlertDefinition alertdef,
                                          boolean force)
        throws RemoveException, PermissionException {
        StopWatch watch = new StopWatch();
        
        if (!force) {
            // If there are any children, delete them, too
            watch.markTimeBegin("delete children");
            for (Iterator it = alertdef.getChildrenBag().iterator(); it.hasNext(); )
            {
                AlertDefinition child = (AlertDefinition) it.next();
                deleteAlertDefinition(subj, child, force);
                it.remove();
            }
            watch.markTimeEnd("delete children");
        }
        
        if (force) {
            // Disassociate from Resource so that the Resource can be deleted
            alertdef.setResource(null);
        }
                
        // Get rid of their triggers first
        watch.markTimeBegin("removeTriggers");
        TriggerDAO tdao = getTriggerDAO();
        tdao.removeTriggers(alertdef);
        watch.markTimeEnd("removeTriggers");

        // Delete escalation state
        watch.markTimeBegin("endEscalation");
        if (alertdef.getEscalation() != null && !alertdef.isResourceTypeDefinition()) {
            EscalationManagerEJBImpl.getOne().endEscalation(alertdef);
        }
        // Disassociated from escalations
        alertdef.setEscalation(null);        
        watch.markTimeEnd("endEscalation");

        watch.markTimeBegin("mark deleted");
        alertdef.setDeleted(true);
        alertdef.setEnabled(false);
        
        for (Iterator it = alertdef.getActions().iterator();
             it.hasNext(); ) {
            Action act = (Action) it.next();
            act.setParent(null);
            act.getChildrenBag().clear();
        }
        
        // Disassociate from parent
        // This must be at the very end since we use the parent to determine 
        // whether or not this is a resource type alert definition.
        alertdef.setParent(null);
        
        watch.markTimeEnd("mark deleted");
        if (log.isDebugEnabled()) {
            log.debug("deleteAlertDefinition: " + watch);
        }

        return true;
    }
    
    /** 
     * Create a new alert definition
     * @ejb:interface-method
     */
    public AlertDefinitionValue createAlertDefinition(AuthzSubjectValue subj,
                                                      AlertDefinitionValue a)
        throws AlertDefinitionCreateException, ActionCreateException,
               FinderException, PermissionException 
    {
        if (EventConstants.TYPE_ALERT_DEF_ID.equals(a.getParentId())) {
            canManageAlerts(subj, new AppdefEntityTypeID(a.getAppdefType(),
                                                         a.getAppdefId()));
        // Subject permissions should have already been checked when creating 
        // the parent (resource type) alert definition.
        } else if (!a.parentIdHasBeenSet()) {
            canManageAlerts(subj, new AppdefEntityID(a.getAppdefType(),
                                                     a.getAppdefId()));
        }
        
        // HHQ-1054: since the alert definition mtime is managed explicitly, 
        // let's initialize it
        a.initializeMTimeToNow();
        
        AlertDefinition res = new AlertDefinition();
        TriggerDAO tDAO = getTriggerDAO();
        ActionDAO aDAO = getActionDAO();
        AlertDefinitionDAO adDAO = getAlertDefDAO();
        
        // The following is duplicated out of what the EJBImpl did.  Makes sense
        a.cleanAction();
        a.cleanCondition();
        a.cleanTrigger();
        adDAO.setAlertDefinitionValue(res, a);
        
        // Create new conditions
        AlertConditionValue[] conds = a.getConditions();
        AlertConditionDAO acDAO = getConditionDAO();
        for (int i = 0; i < conds.length; i++) {
            RegisteredTrigger trigger = conds[i].getTriggerId() != null ?
                tDAO.findById(conds[i].getTriggerId()) : null;

            AlertCondition cond = res.createCondition(conds[i], trigger);
            acDAO.save(cond);
        }
                
        // Create actions
        ActionValue[] actions = a.getActions();
        ActionDAO actDAO = DAOFactory.getDAOFactory().getActionDAO();
        for (int i = 0; i < actions.length; i++) {
            Action parent = null;
            
            if (actions[i].getParentId() != null)
                parent = aDAO.findById(actions[i].getParentId());
            
            Action act = res.createAction(actions[i], parent);
            actDAO.save(act);
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
                trig.setAlertDefinition(res);
            }
        }

        Integer esclId = a.getEscalationId();
        if (esclId != null) {
            Escalation escalation = 
                EscalationManagerEJBImpl.getOne().findById(esclId);
            adDAO.setAlertDefinitionValueNoRels(res, a);
            res.setEscalation(escalation);
        }
        // Alert definitions are the root of the cascade relationship, so
        // we must explicitly save them
        adDAO.save(res);
        return res.getAlertDefinitionValue();
    }
    
    /**
     * Update just the basics
     * @throws PermissionException 
     *
     * @ejb:interface-method
     */
    public void updateAlertDefinitionBasic(AuthzSubjectValue subj, Integer id,
                                           String name, String desc,
                                           int priority, boolean enabled)
        throws PermissionException
    {
        List alertdefs = new ArrayList(1);
        AlertDefinition def = getAlertDefDAO().findById(id);
        canManageAlerts(subj, def);
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
        AlertDefinitionDAO dao = getAlertDefDAO();
        ActionDAO actDao = getActionDAO();
        AlertDefinition aldef = dao.findById(adval.getId());
        
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
                RegisteredTrigger trigger = null;
                
                // Trigger ID is null for resource type alerts
                if (conds[i].getTriggerId() != null) 
                    trigger = getTriggerDAO().findById(conds[i].getTriggerId());
                
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
                
                actDao.save(aldef.createAction(actions[i], parent));
            }
        }

        // Find out the last trigger ID (bizapp should have created them)
        RegisteredTriggerValue[] triggers = adval.getTriggers();
        if (triggers.length > 0) {
            RegisteredTrigger t = null;
            for (int i = 0; i < triggers.length; i++) {
                t = getTriggerDAO().findById(triggers[i].getId());
                t.setAlertDefinition(aldef);
            }

            adval.setActOnTriggerId(t.getId().intValue());
        }

        // Lastly, the modification time
        adval.setMtime(System.currentTimeMillis());

        // Now set the alertdef
        dao.setAlertDefinitionValueNoRels(aldef, adval);
        if (adval.isEscalationIdHasBeenSet()) {
            Integer esclId = adval.getEscalationId();
            Escalation escl = 
                EscalationManagerEJBImpl.getOne().findById(esclId);
            
            aldef.setEscalation(escl);
        }
        
        // Alert definitions are the root of the cascade relationship, so
        // we must explicitly save them
        dao.save(aldef);
        return aldef.getAlertDefinitionValue();
    }

    /** 
     * Enable/Disable alert definitions
     * @ejb:interface-method
     */
    public void updateAlertDefinitionsEnable(AuthzSubjectValue subj,
                                             Integer[] ids, boolean enable)
        throws FinderException, PermissionException 
    {
        List alertdefs = new ArrayList();
        
        for (int i = 0; i < ids.length; i++) {
            AlertDefinition alert = badFindById(ids[i]);

            alertdefs.add(alert);
            
            // If there are any children, add them, too
            alertdefs.addAll(alert.getChildren());
        }

        for (Iterator i = alertdefs.iterator(); i.hasNext(); ) {
            updateAlertDefinitionEnable(subj, (AlertDefinition) i.next(),
                                        enable);
        }
    }

    /** 
     * Enable/Disable alert definitions
     * @ejb:interface-method
     */
    public void updateAlertDefinitionEnable(AuthzSubjectValue subj,
                                            AlertDefinition def, boolean enable)
        throws PermissionException {
        if (def.isEnabled() != enable) {
            canManageAlerts(subj, def);
            def.setEnabled(enable);
            def.setMtime(System.currentTimeMillis());
        }
    }
    
    /** 
     * Enable/Disable alert definitions. For internal use only where the mtime 
     * does not need to be reset on each update.
     * 
     * @return <code>true</code> if the enable/disable succeeded.
     * @ejb:interface-method
     */
    public boolean updateAlertDefinitionInternalEnable(AuthzSubjectValue subj,
                                                       AlertDefinition def, 
                                                       boolean enable)
        throws PermissionException {
        
        boolean succeeded = false;
        
        if (def.isEnabled() != enable) {
            canManageAlerts(subj, def);
            def.setEnabled(enable);
            succeeded = true;
        }
        
        return succeeded;
    }
    
    /** 
     * Enable/Disable alert definitions. For internal use only where the mtime 
     * does not need to be reset on each update.
     * 
     * @return <code>true</code> if the enable/disable succeeded.
     * @ejb:interface-method
     */
    public boolean updateAlertDefinitionInternalEnable(AuthzSubjectValue subj,
                                                       Integer defId, 
                                                       boolean enable)
        throws FinderException, PermissionException {
        
        AlertDefinition def = badFindById(defId);
        
        return updateAlertDefinitionInternalEnable(subj, def, enable);
    }
    
    /** 
     * Set the escalation on the alertdefinition
     * 
     * @ejb:interface-method
     */
    public void setEscalation(AuthzSubjectValue subj, Integer defId,
                              Integer escId)
        throws PermissionException 
    {
        AlertDefinition def = getAlertDefDAO().findById(defId);
        canManageAlerts(subj, def);

        Escalation escl = 
            EscalationManagerEJBImpl.getOne().findById(escId);

        def.setEscalation(escl);
        def.setMtime(System.currentTimeMillis());
    }

    /** 
     * Remove alert definitions
     * @ejb:interface-method
     */
    public void deleteAlertDefinitions(AuthzSubjectValue subj, Integer[] ids)
        throws RemoveException, PermissionException
    {
        for (int i = 0; i < ids.length; i++) {
            AlertDefinition alertdef = getAlertDefDAO().findById(ids[i]);
            
            // Don't delete child alert definitions
            if (alertdef.getParent() != null &&
                !EventConstants.TYPE_ALERT_DEF_ID
                    .equals(alertdef.getParent().getId())) {
                continue;
            }
            
            canManageAlerts(subj, alertdef);
            
            deleteAlertDefinition(subj, alertdef, false);
        }
    }

    /** Remove alert definitions
     * @throws PermissionException 
     * @ejb:interface-method
     */
    public void deleteAlertDefinitions(AuthzSubjectValue subj,
                                       AppdefEntityID aeid)
        throws RemoveException, PermissionException 
    {
        canManageAlerts(subj, aeid);

        AlertDefinitionDAO aDao = getAlertDefDAO();
        List adefs = aDao.findByAppdefEntity(aeid.getType(), aeid.getID());
        
        for (Iterator i = adefs.iterator(); i.hasNext(); ) {
            AlertDefinition adef = (AlertDefinition) i.next();
            
            // First check to see if need to remove from parent
            if (adef.getParent() != null) {
                adef.getParent().removeChild(adef);
            }
            
            deleteAlertDefinition(subj, adef, true);
        }
    }
    
    /**
     * Set Resource to null on appdef entity's alert definitions
     * @ejb:interface-method
     */
    public void disassociateResource(AppdefEntityID aeid) {
        AlertDefinitionDAO aDao = getAlertDefDAO();
        List adefs = aDao.findAllByEntity(aeid);

        for (Iterator i = adefs.iterator(); i.hasNext(); ) {
            AlertDefinition alertdef = (AlertDefinition) i.next();
            alertdef.setResource(null);
        }
        aDao.getSession().flush();
    }
    
    /** Clean up alert definitions and alerts for removed resources
     * 
     * @ejb:interface-method
     */
    public void cleanupAlertDefinitions(AppdefEntityID aeid) {
        StopWatch watch = new StopWatch();
        
        AlertDefinitionDAO aDao = getAlertDefDAO();
        List adefs = aDao.findAllByEntity(aeid);
        
        for (Iterator i = adefs.iterator(); i.hasNext(); ) {
            AlertDefinition alertdef = (AlertDefinition) i.next();
            AlertDAO dao = getAlertDAO();

            // Delete the alerts
            watch.markTimeBegin("deleteByAlertDefinition");
            dao.deleteByAlertDefinition(alertdef);
            watch.markTimeEnd("deleteByAlertDefinition");

            // Remove the conditions
            watch.markTimeBegin("removeConditions");
            getConditionDAO().removeConditions(alertdef);
            watch.markTimeEnd("deleteByAlertDefinition");

            // Remove the actions
            watch.markTimeBegin("removeActions");
            getActionDAO().removeActions(alertdef);
            watch.markTimeEnd("removeActions");

            // Actually remove the definition
            watch.markTimeBegin("remove");
            getAlertDefDAO().remove(alertdef);
            watch.markTimeBegin("remove");

            if (log.isDebugEnabled()) {
                log.debug("deleteAlertDefinition: " + watch);
            }
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
    
    private AlertDefinition badFindById(Integer id, boolean refresh) 
    throws FinderException
    {
        try {
            return getAlertDefDAO().findById(id, refresh);
        } catch(ObjectNotFoundException e) {
            throw new FinderException("Couldn't find AlertDefinition#" + id);
        }
    }
    
    /** Find an alert definition and return a value object
     * @throws PermissionException if user does not have permission to manage
     * alerts
     * @ejb:interface-method
     */
    public AlertDefinitionValue getById(AuthzSubjectValue subj, Integer id) 
        throws FinderException, PermissionException
    {
        return getByIdAndCheck(subj, id).getAlertDefinitionValue();
    }
    
    /** Find an alert definition
     * @throws PermissionException if user does not have permission to manage
     * alerts
     * @ejb:interface-method
     */
    public AlertDefinition getByIdAndCheck(AuthzSubjectValue subj, Integer id)
        throws FinderException, PermissionException
    {
        AlertDefinition ad = badFindById(id);
        if (ad.getParent() != null &&
            !EventConstants.TYPE_ALERT_DEF_ID.equals(ad.getParent().getId()))
            canManageAlerts(subj, getAppdefEntityID(ad));
        return ad;
    }
    
    /** Find an alert definition and return a basic value.  This is called by
     * the abstract trigger, so it does no permission checking.
     * 
     * @param id The alert def Id.
     * @param refresh <code>true</code> to force the alert def state to be 
     *                to be re-read from the database; <code>false</code> to 
     *                allow the persistence engine to return a cached copy.
     * @ejb:interface-method
     */
    public AlertDefinition getByIdNoCheck(Integer id, boolean refresh) 
        throws FinderException {
        return badFindById(id, refresh);
    }
    
    /** 
     * Decide if a trigger should fire the alert
     *
     * @ejb:interface-method
     * @param tid the trigger ID
     * @return the ID of the alert definition
     */
    public Integer getIdFromTrigger(Integer tid) {
        RegisteredTrigger trigger = getTriggerDAO().get(tid);
        if (trigger == null) {
            return null;
        }
        
        AlertDefinition def = trigger.getAlertDefinition();
        
        if (def != null && def.isEnabled() && !def.isDeleted()) {
            return def.getId();
        } else {
            return null;
        }
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
        return getAlertDefDAO().findChildAlertDef(id, parentId) != null;
    }

    /** 
     * Get list of all alert conditions
     * 
     * @return a PageList of {@link AlertDefinitionValue} objects
     * @ejb:interface-method
     */
    public PageList findAllAlertDefinitions(AuthzSubjectValue subj) {
        List vals = new ArrayList();
        
        for (Iterator i = getAlertDefDAO().findAll().iterator(); i.hasNext();) {
            AlertDefinition a = (AlertDefinition) i.next();
            try {
                // Only return the alert definitions that user can see
                canManageAlerts(subj, getAppdefEntityID(a));
            } catch (PermissionException e) {
                continue;
            }
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
        Collection ads = def.getChildren();
        for (Iterator i=ads.iterator(); i.hasNext(); ) {
            AlertDefinition child = (AlertDefinition)i.next();
            vals.add(child.getAlertDefinitionValue());
        }

        return new PageList(vals, vals.size());
    }

    /** 
     * Get the resource-specific alert definition ID by parent ID.
     * 
     * @param aeid The resource.
     * @param pid The ID of the resource type alert definition (parent ID).
     * @return The alert definition ID or <code>null</code> if no alert definition 
     *         is found for the resource.
     * @ejb:interface-method
     */
    public Integer findChildAlertDefinitionId(AppdefEntityID aeid,
                                              Integer pid) {
        AlertDefinition def = getAlertDefDAO().findChildAlertDef(aeid, pid);
        
        return def == null ? null : def.getId();
    }
    
    /** 
     * Get the resource-specific alert definition ID by parent ID, allowing for 
     * the query to return a stale copy of the alert definition (for efficiency 
     * reasons).
     * 
     * @param aeid The resource.
     * @param pid The ID of the resource type alert definition (parent ID).
     * @param allowStale <code>true</code> to allow stale copies of an alert 
     *                   definition in the query results; <code>false</code> to 
     *                   never allow stale copies, potentially always forcing a 
     *                   sync with the database.
     * @return The alert definition ID or <code>null</code> if no alert definition 
     *         is found for the resource.
     * @ejb:interface-method
     */
    public Integer findChildAlertDefinitionId(AppdefEntityID aeid,
                                              Integer pid,
                                              boolean allowStale) {
        AlertDefinition def = getAlertDefDAO().findChildAlertDef(aeid, pid, true);
        
        return def == null ? null : def.getId();        
    }
    

    /**
     * Find alert definitions passing the criteria.
     * 
     * @param minSeverity  Specifies the minimum severity that the defs should
     *                     be set for
     * @param enabled      If non-null, specifies the nature of the returned
     *                     definitions (i.e. only return enabled or disabled
     *                     defs)
     * @param excludeTypeBased  If true, exclude any alert definitions 
     *                          associated with a type-based def.
     * @param pInfo        Paging information.  The sort field must be a 
     *                     value from {@link AlertDefSortField}
     * 
     * @ejb:interface-method
     */
    public List findAlertDefinitions(AuthzSubjectValue subj, 
                                     AlertSeverity minSeverity, Boolean enabled,
                                     boolean excludeTypeBased, PageInfo pInfo)
    {
        return getAlertDefDAO().findDefinitions(subj, minSeverity, enabled, 
                                                excludeTypeBased, pInfo);
    }
    
    /** 
     * Get the list of type-based alert definitions.
     *
     * @param enabled If non-null, specifies the nature of the returned defs.
     * @param pInfo Paging information.  The sort field must be a value from
     *              {@link AlertDefSortField}
     * @ejb:interface-method
     */
    public List findTypeBasedDefinitions(AuthzSubjectValue subj, 
                                         Boolean enabled, PageInfo pInfo) 
        throws PermissionException
    {
        if (!PermissionManagerFactory.getInstance().hasAdminPermission(subj)) {
            throw new PermissionException("Only administrators can do this");
        }
        return getAlertDefDAO().findTypeBased(enabled, pInfo);
    }

    /** 
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(AuthzSubjectValue subj,
                                         AppdefEntityID id, PageControl pc)
        throws PermissionException
    {
        canManageAlerts(subj, id);
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
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(AuthzSubjectValue subj,
                                         AppdefEntityID id,
                                         Integer parentId,
                                         PageControl pc)
        throws PermissionException 
    {
        AlertDefinitionDAO aDao = getAlertDefDAO();

        Collection adefs;
        if (parentId.equals(EventConstants.TYPE_ALERT_DEF_ID)) {
            if (pc.getSortattribute() == SortAttribute.CTIME) {
                adefs =
                    aDao.findByAppdefEntityTypeSortByCtime(id,
                                                           pc.isAscending());
            }
            else {
                adefs = aDao.findByAppdefEntityType(id, pc.isAscending());
            }
        } else {
            canManageAlerts(subj, id);
            AlertDefinition def = getAlertDefDAO().findById(parentId);
            adefs = def.getChildren();
        }
                
        return _valuePager.seek(adefs, pc.getPagenum(), pc.getPagesize());
    }

    /**
     * Get list of alert definition POJOs for a resource
     * @throws PermissionException if user cannot manage alerts for resource
     * @ejb:interface-method
     */
    public List findAlertDefinitions(AuthzSubjectValue subj, AppdefEntityID id)
        throws PermissionException {
        canManageAlerts(subj, id);
        return getAlertDefDAO().findByAppdefEntity(id.getType(), id.getID());
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

    /** 
     * Get list of alert definition names for a resource
     * @ejb:interface-method
     */
    public SortedMap findAlertDefinitionNames(AuthzSubjectValue subj,
                                              AppdefEntityID id,
                                              Integer parentId)
        throws PermissionException 
    {
        AlertDefinitionDAO aDao = getAlertDefDAO();
        TreeMap ret = new TreeMap();
        Collection adefs;
            
        if (parentId != null) {
            if (EventConstants.TYPE_ALERT_DEF_ID.equals(parentId)) {
                adefs = aDao.findByAppdefEntityType(id, true);
            }
            else  {
                AlertDefinition def = getAlertDefDAO().findById(parentId);
                adefs = def.getChildren();
            }
        } else {
            canManageAlerts(subj, id);
            adefs = aDao.findByAppdefEntity(id.getType(), id.getID());
        }
            
        // Use name as key so that map is sorted
        for (Iterator i = adefs.iterator(); i.hasNext(); ) {
            AlertDefinition adLocal = (AlertDefinition) i.next();
            ret.put(adLocal.getName(), adLocal.getId());
        }
        return ret;
    }

    public static AlertDefinitionManagerLocal getOne() {
        try {
            return AlertDefinitionManagerUtil.getLocalHome().create(); 
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    /**
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
