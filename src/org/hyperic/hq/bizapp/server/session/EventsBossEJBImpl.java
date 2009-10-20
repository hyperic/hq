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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javax.ejb.FinderException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.server.session.AppdefResourceType;
import org.hyperic.hq.appdef.server.session.ResourceDeletedZevent;
import org.hyperic.hq.appdef.server.session.ResourceZevent;
import org.hyperic.hq.appdef.shared.AppdefEntityConstants;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityTypeID;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.appdef.shared.AppdefGroupValue;
import org.hyperic.hq.appdef.shared.InvalidAppdefTypeException;
import org.hyperic.hq.application.HQApp;
import org.hyperic.hq.auth.shared.SessionException;
import org.hyperic.hq.auth.shared.SessionManager;
import org.hyperic.hq.auth.shared.SessionNotFoundException;
import org.hyperic.hq.auth.shared.SessionTimeoutException;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceDeleteCallback;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.authz.server.session.ResourceGroupManagerEJBImpl;
import org.hyperic.hq.authz.server.session.SubjectRemoveCallback;
import org.hyperic.hq.authz.shared.AuthzConstants;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.authz.shared.PermissionManagerFactory;
import org.hyperic.hq.authz.shared.ResourceGroupManagerLocal;
import org.hyperic.hq.bizapp.server.trigger.conditional.ConditionalTriggerInterface;
import org.hyperic.hq.bizapp.shared.EventsBossLocal;
import org.hyperic.hq.bizapp.shared.EventsBossUtil;
import org.hyperic.hq.common.ApplicationException;
import org.hyperic.hq.common.DuplicateObjectException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.VetoException;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationAlertType;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.escalation.server.session.PerformsEscalations;
import org.hyperic.hq.escalation.shared.EscalationManagerLocal;
import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.hq.events.ActionCreateException;
import org.hyperic.hq.events.ActionExecuteException;
import org.hyperic.hq.events.ActionInterface;
import org.hyperic.hq.events.AlertConditionCreateException;
import org.hyperic.hq.events.AlertDefinitionCreateException;
import org.hyperic.hq.events.AlertDefinitionInterface;
import org.hyperic.hq.events.AlertInterface;
import org.hyperic.hq.events.AlertNotFoundException;
import org.hyperic.hq.events.AlertSeverity;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.MaintenanceEvent;
import org.hyperic.hq.events.TriggerCreateException;
import org.hyperic.hq.events.ext.RegisterableTriggerInterface;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.ActionManagerEJBImpl;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertDefinitionManagerEJBImpl;
import org.hyperic.hq.events.server.session.AlertManagerEJBImpl;
import org.hyperic.hq.events.server.session.AlertSortField;
import org.hyperic.hq.events.server.session.ClassicEscalationAlertType;
import org.hyperic.hq.events.server.session.EventsStartupListener;
import org.hyperic.hq.events.server.session.RegisteredTriggerManagerEJBImpl;
import org.hyperic.hq.events.server.session.TriggersCreatedZevent;
import org.hyperic.hq.events.shared.ActionManagerLocal;
import org.hyperic.hq.events.shared.ActionValue;
import org.hyperic.hq.events.shared.AlertConditionValue;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertDefinitionValue;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.MaintenanceEventManagerInterface;
import org.hyperic.hq.events.shared.RegisteredTriggerManagerLocal;
import org.hyperic.hq.events.shared.RegisteredTriggerValue;
import org.hyperic.hq.galerts.server.session.GalertDef;
import org.hyperic.hq.galerts.server.session.GalertEscalationAlertType;
import org.hyperic.hq.galerts.server.session.GalertLogSortField;
import org.hyperic.hq.galerts.server.session.GalertManagerEJBImpl;
import org.hyperic.hq.galerts.shared.GalertManagerLocal;
import org.hyperic.hq.measurement.MeasurementNotFoundException;
import org.hyperic.hq.measurement.action.MetricAlertAction;
import org.hyperic.hq.measurement.server.session.DefaultMetricEnableCallback;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.zevents.ZeventListener;
import org.hyperic.hq.zevents.ZeventManager;
import org.hyperic.util.ConfigPropertyException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.EncodingException;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.timer.StopWatch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.quartz.SchedulerException;

/**
 * The BizApp's interface to the Events Subsystem
 *
 * @ejb:bean name="EventsBoss"
 *      jndi-name="ejb/bizapp/EventsBoss"
 *      local-jndi-name="LocalEventsBoss"
 *      view-type="both"
 *      type="Stateless"
 *
 * @ejb:transaction type="Required"
 */

public class EventsBossEJBImpl
    extends BizappSessionEJB
    implements SessionBean
{
    private Log _log = LogFactory.getLog(EventsBossEJBImpl.class);
    private final String BUNDLE = "org.hyperic.hq.bizapp.Resources";

    private SessionManager manager;

    public EventsBossEJBImpl() {
        manager = SessionManager.getInstance();
    }

    private EscalationManagerLocal getEscMan() {
        return EscalationManagerEJBImpl.getOne();
    }

    private RegisteredTriggerManagerLocal getRTM() {
        return RegisteredTriggerManagerEJBImpl.getOne();
    }

    private AlertManagerLocal getAM() {
        return AlertManagerEJBImpl.getOne();
    }

    private AlertDefinitionManagerLocal getADM() {
        return AlertDefinitionManagerEJBImpl.getOne();
    }

    private ActionManagerLocal getActMan() {
        return ActionManagerEJBImpl.getOne();
    }

    private MaintenanceEventManagerInterface getMaintEvtMgr() {
    	return PermissionManagerFactory.getInstance()
    	        .getMaintenanceEventManager();
    }



    /*
     * How the Boss figures out which triggers to create based on conditions
     */
    private void createTriggers(AuthzSubject subject,
                                AlertDefinitionValue alertdef)
        throws TriggerCreateException, InvalidOptionException,
               InvalidOptionValueException {
       getRTM().createTriggers(subject, alertdef);
    }

    /**
     * Clone the parent conditions into the alert definition.
     *
     * @param subject The subject.
     * @param id The entity to which the alert definition is assigned.
     * @param adval The alert definition where the cloned conditions are set.
     * @param conds The parent conditions to clone.
     * @param failSilently <code>true</code> fail silently if cloning fails
     *                     because no measurement is found corresponding
     *                     to the measurement template specified in a parent
     *                     condition; <code>false</code> to throw a
     *                     {@link MeasurementNotFoundException} when this occurs.
     * @return <code>true</code> if cloning succeeded;
     *         <code>false</code> if cloning failed.
     */
    private boolean cloneParentConditions(AuthzSubject subject,
                                          AppdefEntityID id,
                                          AlertDefinitionValue adval,
                                          AlertConditionValue[] conds,
                                          boolean failSilently)
        throws MeasurementNotFoundException {
        // scrub and copy the parent's conditions
        adval.removeAllConditions();

        for (int i = 0; i < conds.length; i++) {
            AlertConditionValue clone = new AlertConditionValue(conds[i]);

            switch (clone.getType()) {
            case EventConstants.TYPE_THRESHOLD:
            case EventConstants.TYPE_BASELINE:
            case EventConstants.TYPE_CHANGE:
                Integer tid = new Integer(clone.getMeasurementId());

                // Don't need to synch the Measurement with the db
                // since changes to the Measurement aren't cascaded
                // on saving the AlertCondition.
                try {
                    Measurement dmv =
                        getMetricManager().findMeasurement(subject, tid,
                            id.getId(), true);
                    clone.setMeasurementId(dmv.getId().intValue());
                } catch (MeasurementNotFoundException e) {
                    _log.error("No measurement found for entity "+id+
                               " associated with template id="+tid+
                               ". Alert definition name ["+adval.getName()+"]");
                    _log.debug("Root cause", e);

                    if (failSilently) {
                        _log.info("Alert condition creation failed. " +
                                "The alert definition for entity "+id+
                                " with name ["+adval.getName()+
                                "] should not be created.");
                        // Just set to 0, it'll never fire
                        clone.setMeasurementId(0);
                        return false;
                    } else {
                        throw e;
                    }
                }

                break;
            case EventConstants.TYPE_ALERT:

                // Don't need to synch the child alert definition Id lookup.
                Integer recoverId =
                    getADM().findChildAlertDefinitionId(id,
                                      new Integer(clone.getMeasurementId()),
                                      true);

                if (recoverId == null) {
                    // recoverId should never be null, but if it is and assertions
                    // are disabled, just move on.
                    assert false : "recover Id should not be null.";

                    _log.error("A recovery alert has no associated recover " +
                               "from alert. Setting alert condition " +
                               "measurement Id to 0.");
                    clone.setMeasurementId(0);
                } else {
                    clone.setMeasurementId(recoverId.intValue());
                }

                break;
            }

            // Now add it to the alert definition
            adval.addCondition(clone);
        }

        return true;
    }


    private void cloneParentActions(AppdefEntityID id,
                                    AlertDefinitionValue child,
                                    ActionValue[] actions) {
        child.removeAllActions();
        for (int i = 0; i < actions.length; i++) {
            ActionValue childAct;
            try {
                ActionInterface actInst = (ActionInterface)
                    Class.forName(actions[i].getClassname()).newInstance();
                ConfigResponse config =
                    ConfigResponse.decode(actions[i].getConfig());
                actInst.setParentActionConfig(id, config);
                childAct =
                    new ActionValue(null, actInst.getImplementor(),
                                    actInst.getConfigResponse().encode(),
                                    actions[i].getId());
            } catch (Exception e) {
                // Not a valid action, skip it then
                _log.debug(actions[i].getClassname(), e);
                continue;
            }
            child.addAction(childAct);
        }
    }

    /**
     * Get the number of alerts for the given array of AppdefEntityID's
     * @ejb:interface-method
     */
    public int[] getAlertCount(int sessionID, AppdefEntityID[] ids)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException, FinderException {
        AuthzSubject subject = manager.getSubject(sessionID);

        int[] counts = getAM().getAlertCount(ids);
        counts = GalertManagerEJBImpl.getOne().fillAlertCount(subject, ids,
                                                              counts);
        return counts;
    }

    /**
     * Create an alert definition
     *
     * @ejb:interface-method
     */
    public AlertDefinitionValue createAlertDefinition(int sessionID,
                                                     AlertDefinitionValue adval)
        throws AlertDefinitionCreateException,
               PermissionException, InvalidOptionException,
               InvalidOptionValueException,
               SessionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);

        // Verify that there are some conditions to evaluate
        if (adval.getConditions().length == 0) {
            throw new AlertDefinitionCreateException("Conditions cannot be " +
                                                     "null or empty");

        }

        // Create list of ID's to create the alert definition
        List appdefIds = new ArrayList();
        final AppdefEntityID aeid = getAppdefEntityID(adval);
        appdefIds.add(aeid);
        if (aeid.isGroup()) {
            // Look up the group
            AppdefGroupValue group;
            try {
                group = getAppdefBoss().findGroup(sessionID,
                                                  adval.getAppdefId());
            } catch (InvalidAppdefTypeException e) {
                throw new AlertDefinitionCreateException(e);
            }

            appdefIds.addAll(group.getAppdefGroupEntries());
        }

        ArrayList triggers = new ArrayList();

        AlertDefinitionValue parent = null;
        AlertDefinitionManagerLocal adm = getADM();
        // Iterate through to create the appropriate triggers and alertdef
        for (Iterator it = appdefIds.iterator(); it.hasNext(); ) {
            AppdefEntityID id = (AppdefEntityID) it.next();

            // Reset the value object with this entity ID
            adval.setAppdefType(id.getType());
            adval.setAppdefId(id.getId());

            // Scrub the triggers just in case
            adval.removeAllTriggers();

            if (!id.isGroup()) {
                // If this is for the members of a group, we need to
                // scrub and copy the parent's conditions
                if (parent != null) {
                    adval.setParentId(parent.getId());
                    try {
                        cloneParentConditions(subject, id, adval,
                                              parent.getConditions(),
                                              false);
                    } catch (MeasurementNotFoundException e) {
                        throw new AlertConditionCreateException(e);
                    }
                }

                // Create the triggers
                createTriggers(subject, adval);
                triggers.addAll(Arrays.asList(adval.getTriggers()));
            }

            // Create a measurement AlertLogAction if necessary
            setMetricAlertAction(adval);

            // Now create the alert definition
            AlertDefinitionValue created =
                adm.createAlertDefinition(subject, adval);

            if (parent == null)
                parent = created;
        }

        return parent;
    }

    /**
     * Create an alert definition for a resource type
     *
     * @ejb:interface-method
     */
    public AlertDefinitionValue createResourceTypeAlertDefinition(
        int sessionID, AppdefEntityTypeID aetid, AlertDefinitionValue adval)
        throws AlertDefinitionCreateException,
               PermissionException, InvalidOptionException,
               InvalidOptionValueException,
               SessionNotFoundException, SessionTimeoutException {
        
        final boolean debug = _log.isDebugEnabled();
        StopWatch watch = new StopWatch();
        
        AuthzSubject subject = manager.getSubject(sessionID);

        // Verify that there are some conditions to evaluate
        if (adval.getConditions().length == 0) {
            throw new AlertDefinitionCreateException(
                "Conditions cannot be null or empty");
        }

        AlertDefinitionValue parent;

        // Create the parent alert definition
        adval.setAppdefType(aetid.getType());
        adval.setAppdefId(aetid.getId());
        adval.setParentId(EventConstants.TYPE_ALERT_DEF_ID);

        // Now create the alert definition
        if (debug) watch.markTimeBegin("createParentAlertDefinition");
        parent = getADM().createAlertDefinition(subject, adval);
        if (debug) watch.markTimeEnd("createParentAlertDefinition");

        adval.setParentId(parent.getId());

        if (debug) watch.markTimeBegin("lookupResources");

        // Lookup resources
        Integer[] entIds;
        switch (aetid.getType()) {
        case AppdefEntityConstants.APPDEF_TYPE_PLATFORM:
            entIds =
                getPlatformManager().getPlatformIds(subject, aetid.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVER:
            entIds =
                getServerManager().getServerIds(subject, aetid.getId());
            break;
        case AppdefEntityConstants.APPDEF_TYPE_SERVICE:
            entIds =
                getServiceManager().getServiceIds(subject, aetid.getId());
            break;
        default:
            throw new InvalidOptionException(
                "Alerts cannot be defined on appdef entity type " +
                aetid.getType());
        }

        if (debug) watch.markTimeEnd("lookupResources");

        List zevents = new ArrayList(entIds.length);

        if (debug) watch.markTimeBegin("createChildAlertDefinitions[" + entIds.length + "]");

        // Iterate through to create the appropriate triggers and alertdef
        AlertDefinitionManagerLocal adm = getADM();
        RegisteredTriggerManagerLocal rtm = getRTM();
        for (int ei = 0; ei < entIds.length; ei++) {
            StopWatch childWatch = new StopWatch();

            AppdefEntityID id = new AppdefEntityID(aetid.getType(), entIds[ei]);

            // Reset the value object with this entity ID
            adval.setAppdefId(id.getId());

            // Scrub the triggers just in case
            adval.removeAllTriggers();

            try {
                boolean succeeded =
                    cloneParentConditions(subject, id, adval,
                                          parent.getConditions(), true);

                if (!succeeded) {
                    continue;
                }
            } catch (MeasurementNotFoundException e) {
                throw new AlertDefinitionCreateException(
                        "Expected parent condition cloning to fail silently", e);
            }

            // Create the triggers
            if (debug) childWatch.markTimeBegin("createTriggers");
            // HHQ-3423: Do not add the TransactionListener here.
            // Add it at the end after all the triggers are created.
            rtm.createTriggers(subject, adval, false);
            if (debug) childWatch.markTimeEnd("createTriggers");

            // Make sure the actions have the proper parentId
            cloneParentActions(id, adval, parent.getActions());

            // Create a measurement AlertLogAction if necessary
            setMetricAlertAction(adval);

            // Now create the alert definition
            if (debug) childWatch.markTimeBegin("createAlertDefinition");
            AlertDefinitionValue newAdval = adm.createAlertDefinition(subject, adval);            
            if (debug) {
                childWatch.markTimeEnd("createAlertDefinition");
                _log.debug("createChildAlertDefinition[" + id + "]: time=" + childWatch);
            }
            zevents.add(new TriggersCreatedZevent(newAdval.getId()));
        }
        
        if (debug) watch.markTimeEnd("createChildAlertDefinitions[" + entIds.length + "]");
        
        // HHQ-3423: Add the TransactionListener after all the triggers are created
        if (!zevents.isEmpty()) {
            if (debug) watch.markTimeBegin("addTriggersCreatedTxListener");
            rtm.addTriggersCreatedTxListener(zevents);
            if (debug) watch.markTimeEnd("addTriggersCreatedTxListener");
        }
        
        if (debug) {            
            _log.debug("createResourceTypeAlertDefinition: time=" + watch);
        }
        
        return parent;
    }

    private void setMetricAlertAction(AlertDefinitionValue adval) {
        AlertConditionValue[] conds = adval.getConditions();
        for (int i = 0; i < conds.length; i++) {
            if (conds[i].getType() == EventConstants.TYPE_THRESHOLD ||
                conds[i].getType() == EventConstants.TYPE_BASELINE  ||
                conds[i].getType() == EventConstants.TYPE_CHANGE)
            {
                ActionValue action = new ActionValue();
                action.setClassname(MetricAlertAction.class.getName());

                ConfigResponse config = new ConfigResponse();
                try {
                    action.setConfig(config.encode());
                } catch (EncodingException e) {
                    // This should never happen
                    _log.error("Empty ConfigResponse threw an encoding error",
                              e);
                }

                adval.addAction(action);
                break;
            }
        }
    }

    /**
     * @ejb:interface-method
     */
    public void inheritResourceTypeAlertDefinition(AuthzSubject subject,
                                                   AppdefEntityID id)
        throws AppdefEntityNotFoundException, PermissionException,
               InvalidOptionException, InvalidOptionValueException,
               AlertDefinitionCreateException
    {
        AppdefEntityValue rv = new AppdefEntityValue(id, subject);
        AppdefResourceType type = rv.getAppdefResourceType();

        // Find the alert definitions for the type
        AppdefEntityTypeID aetid =
            new AppdefEntityTypeID(type.getAppdefType(), type.getId());

        // The alert definitions should be returned sorted by creation time.
        // This should minimize the possibility of creating a recovery alert
        // before the recover from alert.
        PageControl pc = new PageControl(0,
                                         PageControl.SIZE_UNLIMITED,
                                         PageControl.SORT_ASC,
                                         SortAttribute.CTIME);

        List defs = getADM().findAlertDefinitions(subject, aetid, pc);

        AlertDefinitionManagerLocal adm = getADM();
        ArrayList triggers = new ArrayList();
        for (Iterator it = defs.iterator(); it.hasNext(); ) {
            AlertDefinitionValue adval = (AlertDefinitionValue) it.next();

            // Only create if definition does not already exist
            if (getADM().isAlertDefined(id, adval.getId()))
                continue;

            // Set the parent ID
            adval.setParentId(adval.getId());

            // Reset the value object with this entity ID
            adval.setAppdefId(id.getId());

            try {
                boolean succeeded =
                    cloneParentConditions(subject, id, adval, adval.getConditions(), true);

                if (!succeeded) {
                    continue;
                }
            } catch (MeasurementNotFoundException e) {
                throw new AlertDefinitionCreateException(
                        "Expected parent condition cloning to fail silently", e);
            }

            // Create the triggers
            createTriggers(subject, adval);
            triggers.addAll(Arrays.asList(adval.getTriggers()));

            // Recreate the actions
            cloneParentActions(id, adval, adval.getActions());

            // Create a measurement AlertLogAction if necessary
            setMetricAlertAction(adval);

            // Now create the alert definition
            adm.createAlertDefinition(subject, adval);
        }
    }

    /**
     * @ejb:interface-method
     */
    public Action createAction(int sessionID, Integer adid, String className,
                               ConfigResponse config)
        throws SessionNotFoundException, SessionTimeoutException,
               ActionCreateException, RemoveException, FinderException,
               PermissionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);

        ArrayList alertdefs = new ArrayList();

        // check that the user can actually manage alerts for this resource
        AlertDefinition ad = getADM().getByIdAndCheck(subject, adid);
        alertdefs.add(ad);

        // If there are any children
        alertdefs.addAll(ad.getChildren());

        Action root = null;
        ActionManagerLocal actMan = getActMan();
        for (Iterator it = alertdefs.iterator(); it.hasNext(); ) {
            ad = (AlertDefinition) it.next();

            try {
                if (root == null) {
                    root = actMan.createAction(ad, className, config, null);
                }
                else {
                    actMan.createAction(ad, className, config, root);
                }
            } catch (EncodingException e) {
                throw new SystemException("Couldn't encode.", e);
            }
        }

        return root;
    }

    /**
     * Activate/deactivate a collection of alert definitions
     *
     * @ejb:interface-method
     */
    public void activateAlertDefinitions(int sessionID, Integer[] ids,
                                         boolean activate)
        throws SessionNotFoundException, SessionTimeoutException,
               FinderException, PermissionException {
        AuthzSubject subject = manager.getSubject(sessionID);
        getADM().updateAlertDefinitionsActiveStatus(subject, ids, activate);
    }

    /**
     * Activate or deactivate alert definitions by AppdefEntityID.
     *
     * @ejb:interface-method
     */
    public void activateAlertDefinitions(int sessionID, AppdefEntityID[] eids,
                                         boolean activate)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, PermissionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);

        boolean debugEnabled = _log.isDebugEnabled();
        String status = (activate ? "enabled" : "disabled");
        Resource res = null;

        for (int i=0; i<eids.length; i++) {
            AppdefEntityID eid = eids[i];
            if (debugEnabled) {
                _log.debug("AppdefEntityID [" + eid + "]");
            }
            if (eid.isGroup()) {
                ResourceGroupManagerLocal rgm = ResourceGroupManagerEJBImpl.getOne();
                ResourceGroup group = rgm.findResourceGroupById(eid.getId());

                // Get the group alerts
                GalertManagerLocal gam = GalertManagerEJBImpl.getOne();
                Collection allAlerts = gam.findAlertDefs(group, PageControl.PAGE_ALL);
                for (Iterator it = allAlerts.iterator(); it.hasNext(); ) {
                    GalertDef galertDef = (GalertDef) it.next();
                    gam.enable(galertDef, activate);
                    if (debugEnabled) {
                        _log.debug("Group Alert [" + galertDef + "] " + status);
                    }
                }

                // Get the resource alerts of the group
                Collection resources = rgm.getMembers(group);
                for (Iterator rIter = resources.iterator(); rIter.hasNext(); ) {
                    res = (Resource) rIter.next();
                    updateAlertDefinitionsActiveStatus(subject, res, activate);
                }
            } else {
                res = getResourceManager().findResource(eid);
                updateAlertDefinitionsActiveStatus(subject, res, activate);
            }
        }
    }

    private void updateAlertDefinitionsActiveStatus(AuthzSubject subject,
                                Resource res, boolean activate)
        throws PermissionException
    {
        boolean debugEnabled = _log.isDebugEnabled();
        String status = (activate ? "enabled" : "disabled");

        AlertDefinitionManagerLocal adm = getADM();
        Collection allAlerts = adm.findRelatedAlertDefinitions(subject, res);

        for (Iterator it = allAlerts.iterator(); it.hasNext();) {
            AlertDefinition alertDef = (AlertDefinition) it.next();
            adm.updateAlertDefinitionActiveStatus(subject, alertDef, activate);
            if (debugEnabled) {
                _log.debug("Resource Alert [" + alertDef + "] " + status);
            }
        }
    }

    /**
     * Update just the basics
     *
     * @ejb:interface-method
     */
    public void updateAlertDefinitionBasic(int sessionID, Integer alertDefId,
                                           String name, String desc,
                                           int priority, boolean activate)
        throws SessionNotFoundException, SessionTimeoutException,
               FinderException, RemoveException, PermissionException {
        AuthzSubject subject = manager.getSubject(sessionID);
        getADM().updateAlertDefinitionBasic(subject, alertDefId, name, desc,
                                            priority, activate);
    }

    /**
     * @ejb:interface-method
     */
    public void updateAlertDefinition(int sessionID, AlertDefinitionValue adval)
        throws TriggerCreateException, InvalidOptionException,
               InvalidOptionValueException, AlertConditionCreateException,
               ActionCreateException, FinderException, RemoveException,
               SessionNotFoundException, SessionTimeoutException {

        final boolean debug = _log.isDebugEnabled();
        StopWatch watch = new StopWatch();

        AuthzSubject subject = manager.getSubject(sessionID);

        // Verify that there are some conditions to evaluate
        if (adval.getConditions().length < 1) {
            throw new InvalidOptionValueException(
                "Conditions cannot be null or empty");
        }

        AlertDefinitionManagerLocal adm = getADM();
        RegisteredTriggerManagerLocal rtm = getRTM();

        if (EventConstants.TYPE_ALERT_DEF_ID.equals(adval.getParentId()) ||
            adval.getAppdefType() == AppdefEntityConstants.APPDEF_TYPE_GROUP) {
            // A little more work to do for group and type alert definition
            
            if (debug) watch.markTimeBegin("updateParentAlertDefinition");
            
            adval = adm.updateAlertDefinition(adval);
            
            if (debug) {
                watch.markTimeEnd("updateParentAlertDefinition");
                watch.markTimeBegin("findAlertDefinitionChildren");
            }

            List children = adm.findAlertDefinitionChildren(adval.getId());
            
            if (debug) {
                watch.markTimeEnd("findAlertDefinitionChildren");
                watch.markTimeBegin("updateChildAlertDefinitions[" + children.size() + "]");
            }
            
            List zevents = new ArrayList(children.size());

            for (Iterator it = children.iterator(); it.hasNext();) {
                AlertDefinitionValue child = (AlertDefinitionValue) it.next();

                AppdefEntityID id = new AppdefEntityID(child.getAppdefType(),
                                                       child.getAppdefId());

                // Now add parent's conditions, actions, and new triggers
                try {
                    cloneParentConditions(subject, id, child,
                                          adval.getConditions(),
                                          false);
                } catch (MeasurementNotFoundException e) {
                    throw new AlertConditionCreateException(e);
                }

                cloneParentActions(id, child, adval.getActions());

                // Set the alert definition frequency type
                child.setFrequencyType(adval.getFrequencyType());
                child.setCount(adval.getCount());
                child.setRange(adval.getRange());

                // Set the alert definition filtering options
                child.setWillRecover(adval.getWillRecover());
                child.setNotifyFiltered(adval.getNotifyFiltered());
                child.setControlFiltered(adval.getControlFiltered());

                // Triggers are deleted by the manager
                rtm.deleteTriggers(child.getId());
                
                child.removeAllTriggers();

                // HHQ-3423: Do not add the TransactionListener here.
                // Add it at the end after all the triggers are created.
                rtm.createTriggers(subject, child, false);

                // Now update the alert definition
                AlertDefinitionValue updatedChild = adm.updateAlertDefinition(child);
                
                // Create TriggersCreatedZevent
                zevents.add(new TriggersCreatedZevent(updatedChild.getId()));
            }
            
            if (debug) watch.markTimeEnd("updateChildAlertDefinitions[" + children.size() + "]");
            
            // HHQ-3423: Add the TransactionListener after all the triggers are created
            if (!zevents.isEmpty()) {
                if (debug) watch.markTimeBegin("addTriggersCreatedTxListener");
                rtm.addTriggersCreatedTxListener(zevents);
                if (debug) watch.markTimeEnd("addTriggersCreatedTxListener");
            }
        }
        else {
            // First, get rid of the current triggers
            rtm.deleteTriggers(adval.getId());
            adval.removeAllTriggers();

            // Now create the new triggers
            createTriggers(subject, adval);

            // Now update the alert definition
            adm.updateAlertDefinition(adval);
        }
        
        if (debug) {            
            _log.debug("updateAlertDefinition: time=" + watch);
        }
    }

    /**
     * Get actions for a given alert.
     *
     * @param alertId the alert id
     *
     * @ejb:interface-method
     */
    public List getActionsForAlert(int sessionId, Integer alertId)
        throws SessionNotFoundException, SessionTimeoutException
    {
        manager.authenticate(sessionId);
        return getActMan().getActionsForAlert(alertId.intValue());
    }

    /**
     * Update an action
     *
     * @ejb:interface-method
     */
    public void updateAction(int sessionID, ActionValue aval)
        throws SessionNotFoundException, SessionTimeoutException
    {
        manager.authenticate(sessionID);
        getActMan().updateAction(aval);
    }

    /**
     * Delete a collection of alert definitions
     *
     * @ejb:transaction type="NotSupported"
     * @ejb:interface-method
     */
    public void deleteAlertDefinitions(int sessionID, Integer[] ids)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        
        // HQ-1887: Process each alert definition in a separate transaction
        // to avoid transaction timeout issues, especially with
        // resource type alert definitions
        for (int i=0; i< ids.length; i++) {
            getADM().deleteAlertDefinitions(subject, new Integer[] {ids[i]});
        }
    }

    /**
     * Delete list of alerts
     *
     * @ejb:interface-method
     */
    public void deleteAlerts(int sessionID, Integer[] ids)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException
    {
        manager.authenticate(sessionID);
        getAM().deleteAlerts(ids);
    }

    /**
     * Delete all alerts for a resource
     *
     * @ejb:interface-method
     */
    public int deleteAlerts(int sessionID, AppdefEntityID aeid)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        return getAM().deleteAlerts(subject, aeid);
    }

    /**
     * Delete all alerts for a given period of time
     *
     * @ejb:interface-method
     */
    public int deleteAlerts(int sessionID, long begin, long end)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException
    {
        manager.authenticate(sessionID);
        // XXX - check security
        return getAM().deleteAlerts(begin, end);
    }

    /**
     * Delete all alerts for a list of alert definitions
     * @throws FinderException if alert definition is not found
     *
     * @ejb:interface-method
     */
    public int deleteAlertsForDefinitions(int sessionID, Integer[] adids)
        throws SessionNotFoundException, SessionTimeoutException,
               RemoveException, PermissionException, FinderException
    {
        AuthzSubject subject = manager.getSubject(sessionID);

        // Delete alerts for definition and its children
        int count = 0;
        AlertDefinitionManagerLocal adm = getADM();
        for (int i = 0; i < adids.length; i++) {
            AlertDefinition def = adm.getByIdAndCheck(subject, adids[i]);
            count += getAM().deleteAlerts(subject, def);

            Collection children = def.getChildren();

            for (Iterator it = children.iterator(); it.hasNext(); ) {
                AlertDefinition child = (AlertDefinition) it.next();
                count += getAM().deleteAlerts(subject, child);
            }
        }
        return count;
    }

    /**
     * Get an alert definition by ID
     *
     * @ejb:interface-method
     */
    public AlertDefinitionValue getAlertDefinition(int sessionID, Integer id)
    throws SessionNotFoundException, SessionTimeoutException,
           FinderException, PermissionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        return getADM().getById(subject, id);
    }

    /**
     * Find an alert by ID
     *
     * @ejb:interface-method
     */
    public Alert getAlert(int sessionID, Integer id)
    throws SessionNotFoundException,
           SessionTimeoutException,
           AlertNotFoundException
    {
        manager.authenticate(sessionID);

        Alert alert = getAM().findAlertById(id);

        if (alert == null) throw new AlertNotFoundException(id);

        return alert;
    }

    /**
     * Get a list of all alert definitions
     *
     * @ejb:interface-method
     */
    public PageList findAllAlertDefinitions(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        return getADM().findAllAlertDefinitions(subject);
    }

	/**
     * Get a collection of alert definitions for a resource
     *
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(int sessionID, AppdefEntityID id,
                                         PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        return getADM().findAlertDefinitions(subject, id, pc);
    }

    /**
     * Get a collection of alert definitions for a resource or resource type
     * @ejb:interface-method
     */
    public PageList findAlertDefinitions(int sessionID, AppdefEntityTypeID id,
                                         PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        return getADM().findAlertDefinitions(subject, id, pc);
    }

    /**
     * Find all alert definition names for a resource
     * @return Map of AlertDefinition names and IDs
     * @ejb:interface-method
     */
    public Map findAlertDefinitionNames(int sessionID, AppdefEntityID id,
                                        Integer parentId)
        throws SessionNotFoundException, SessionTimeoutException,
               AppdefEntityNotFoundException, PermissionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        return getADM().findAlertDefinitionNames(subject, id, parentId);
    }

    /**
     * Get a list of all alerts
     *
     * @ejb:interface-method
     */
    public PageList findAllAlerts(int sessionID)
        throws SessionNotFoundException, SessionTimeoutException
    {
        manager.authenticate(sessionID);
        // XXX no security... FIXME
        return getAM().findAllAlerts();
    }

    /**
     * Find all alerts for an appdef resource
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(int sessionID, AppdefEntityID id, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        return getAM().findAlerts(subject, id, pc);
    }

    /**
     * Find all alerts for an appdef resource
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(int sessionID, AppdefEntityID id,
                               long begin, long end, PageControl pc)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        return getAM().findAlerts(subject, id, begin, end, pc);
    }

    /**
     * Search alerts given a set of criteria
     * @param username the username
     * @param count the maximum number of alerts to return
     * @param priority allowable values: 0 (all), 1, 2, or 3
     * @param timeRange the amount of time from current time to include
     * @param ids the IDs of resources to include or null for ALL
     * @return a list of {@link Escalatable}s
     * @ejb:interface-method
     */
    public List findRecentAlerts(String username, int count, int priority,
                                 long timeRange, AppdefEntityID[] ids)
        throws LoginException, ApplicationException, ConfigPropertyException
    {
        int sessionId = getAuthManager().getUnauthSessionId(username);
        return findRecentAlerts(sessionId, count, priority, timeRange, ids);
    }

    /**
     * Search recent alerts given a set of criteria
     * @param sessionID the session token
     * @param count the maximum number of alerts to return
     * @param priority allowable values: 0 (all), 1, 2, or 3
     * @param timeRange the amount of time from current time to include
     * @param ids the IDs of resources to include or null for ALL
     * @return a list of {@link Escalatable}s
     * @ejb:interface-method
     */
    public List findRecentAlerts(int sessionID, int count, int priority,
                                 long timeRange, AppdefEntityID[] ids)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException
    {
        AuthzSubject subject  = manager.getSubject(sessionID);
        long cur = System.currentTimeMillis();

        List appentResources =
            ids != null ? appentResources = Arrays.asList(ids) : null;

        // Assume if user can be alerted, then they can view resource,
        // otherwise, it'll be filtered out later anyways
        List alerts = getAM().findEscalatables(subject, count, priority,
                                               timeRange, cur, appentResources);

        // CheckAlertingScope now only used for galerts
        if (ids == null) {
            // find ALL alertable resources
            appentResources = getPlatformManager().checkAlertingScope(subject);
        }

        GalertManagerLocal gMan = GalertManagerEJBImpl.getOne();
        List galerts = gMan.findEscalatables(subject, count, priority,
                                             timeRange, cur, appentResources);
        alerts.addAll(galerts);

        Collections.sort(alerts, new Comparator() {
            public int compare(Object o1, Object o2) {
                long l1 = ((Escalatable)o1).getAlertInfo().getTimestamp();
                long l2 = ((Escalatable)o2).getAlertInfo().getTimestamp();

                // Reverse sort
                if (l1 > l2)
                    return -1;
                else if (l1 < l2)
                    return 1;
                else
                    return 0;
            }
        });

        Set goodIds = new HashSet();
        List badIds = new ArrayList();

        List res = new ArrayList();
        for (Iterator i = alerts.iterator();
             i.hasNext() && res.size() < count; )
        {
            Escalatable alert = (Escalatable) i.next();
            PerformsEscalations def = alert.getDefinition();
            AlertDefinitionInterface defInfo = def.getDefinitionInfo();
            AppdefEntityID aeid;

            aeid = new AppdefEntityID(defInfo.getResource());

            if (badIds.contains(aeid))
                continue;

            // Check to see if we already have the resource in the hash map
            if (!goodIds.contains(aeid)) {
                AppdefEntityValue entVal = new AppdefEntityValue(aeid, subject);

                try {
                    entVal.getName();
                    goodIds.add(aeid);
                } catch (Exception e) {
                    // Probably because the resource does not exist
                    badIds.add(aeid);
                    continue;
                }
            }

            res.add(alert);
        }

        return res;
    }

    /**
     * Get config schema info for an action class
     *
     * @ejb:interface-method
     */
    public ConfigSchema getActionConfigSchema(int sessionID, String actionClass)
        throws SessionNotFoundException, SessionTimeoutException,
               EncodingException
    {
        manager.authenticate(sessionID);
        ActionInterface iface;
        try {
            Class c = Class.forName(actionClass);
            iface = (ActionInterface) c.newInstance();
        } catch(Exception exc){
            throw new EncodingException("Failed to instantiate class: " + exc);
        }
        return iface.getConfigSchema();
    }

    /*
     * The following trigger API's are specific to the CLI only
     */

    /**
     * Get config schema info for a trigger class
     *
     * @ejb:interface-method
     */
    public ConfigSchema getRegisteredTriggerConfigSchema(int sessionID,
                                                         String triggerClass)
        throws SessionNotFoundException, SessionTimeoutException,
               EncodingException
    {
        manager.authenticate(sessionID);
        RegisterableTriggerInterface iface;
        Class c;

        try {
            c = Class.forName(triggerClass);
            iface = (RegisterableTriggerInterface) c.newInstance();
        } catch(Exception exc){
            throw new EncodingException("Failed to instantiate class: " + exc);
        }
        return iface.getConfigSchema();
    }

    /**
     * @ejb:interface-method
     */
    public void deleteEscalationByName(int sessionID, String name)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, ApplicationException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        Escalation e = getEscMan().findByName(name);

        getEscMan().deleteEscalation(subject, e);
    }

    /**
     * @ejb:interface-method
     */
    public void deleteEscalationById(int sessionID, Integer id)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, ApplicationException
    {
        deleteEscalationById(sessionID, new Integer[]{id});
    }

    /**
     * remove escalation by id
     * @ejb:interface-method
     */
    public void deleteEscalationById(int sessionID, Integer[] ids)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, ApplicationException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        EscalationManagerLocal escMan = getEscMan();

        for (int i=0; i<ids.length; i++) {
            Escalation e = escMan.findById(ids[i]);

            escMan.deleteEscalation(subject, e);
        }
    }

    /**
     * retrieve escalation by alert definition id.
     */
    private Escalation findEscalationByAlertDefId(Integer id,
                                                  EscalationAlertType type)
        throws PermissionException
    {
        return getEscMan().findByDefId(type, id);
    }

    /**
     * retrieve escalation name by alert definition id.
     *
     * @ejb:interface-method
     */
    public Integer getEscalationIdByAlertDefId(int sessionID, Integer id,
                                               EscalationAlertType alertType)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, FinderException
    {
        manager.authenticate(sessionID);
        Escalation esc = findEscalationByAlertDefId(id, alertType);
        return esc == null ? null : esc.getId();
    }

    /**
     * set escalation name by alert definition id.
     *
     * @ejb:interface-method
     */
    public void setEscalationByAlertDefId(int sessionID, Integer id,
                                          Integer escId,
                                          EscalationAlertType alertType)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        manager.authenticate(sessionID);
        Escalation escalation = findEscalationById(sessionID, escId);
        // TODO: check permission
        getEscMan().setEscalation(alertType, id, escalation);
    }

    /**
     * unset escalation by alert definition id.
     *
     * @ejb:interface-method
     */
    public void unsetEscalationByAlertDefId(int sessionID, Integer id,
                                            EscalationAlertType alertType)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        manager.authenticate(sessionID);
        // TODO: check permission
        getEscMan().setEscalation(alertType, id, null);
    }

    /**
     * retrieve escalation JSONObject by alert definition id.
     *
     * @ejb:interface-method
     */
    public JSONObject jsonEscalationByAlertDefId(int sessionID, Integer id,
                                                 EscalationAlertType alertType)
        throws SessionException, PermissionException, JSONException,
               FinderException
    {
        manager.authenticate(sessionID);
        Escalation e = findEscalationByAlertDefId(id, alertType);
        return e == null ? null
                         : new JSONObject().put(e.getJsonName(), e.toJSON());
    }

    /**
     * retrieve escalation object by escalation id.
     *
     * @ejb:interface-method
     */
    public Escalation findEscalationById(int sessionID, Integer id)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        AuthzSubject subject = manager.getSubject(sessionID);
        Escalation e = getEscMan().findById(subject, id);

        // XXX: Temporarily get around lazy loading problem
        e.isPauseAllowed();
        e.getMaxPauseTime();
        return e;
    }

    /**
     * @ejb:interface-method
     */
    public void addAction(int sessionID, Escalation e,
                          ActionConfigInterface cfg, long waitTime)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        manager.authenticate(sessionID);
        getEscMan().addAction(e, cfg, waitTime);
    }

    /**
     * @ejb:interface-method
     */
    public void removeAction(int sessionID, Integer escId, Integer actId)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        manager.authenticate(sessionID);
        Escalation e = getEscMan().findById(escId);

        if (e != null) {
            getEscMan().removeAction(e, actId);
        }
    }

    /**
     * Retrieve a list of {@link EscalationState}s, representing the active
     * escalations in the system.
     *
     * @ejb:interface-method
     */
    public List getActiveEscalations(int sessionId, int maxEscalations)
        throws SessionException
    {
        manager.authenticate(sessionId);

        return getEscMan().getActiveEscalations(maxEscalations);
    }

    /**
     * Gets the escalatable associated with the specified state
     * @ejb:interface-method
     */
    public Escalatable getEscalatable(int sessionId, EscalationState state)
        throws SessionException
    {
        manager.authenticate(sessionId);
        return getEscMan().getEscalatable(state);
    }

    /**
     * retrieve all escalation policy names as a Array of JSONObject.
     *
     * Escalation json finders begin with json* to be consistent with
     * DAO finder convention
     *
     * @ejb:interface-method
     */
    public JSONArray listAllEscalationName(int sessionID)
        throws JSONException, SessionTimeoutException, SessionNotFoundException,
               PermissionException
    {
        AuthzSubject  subject = manager.getSubject(sessionID);
        Collection all = getEscMan().findAll(subject);
        JSONArray jarr = new JSONArray();
        for (Iterator i = all.iterator(); i.hasNext(); ) {
            Escalation esc = (Escalation)i.next();
            jarr.put(new JSONObject()
                .put("id", esc.getId())
                .put("name", esc.getName()));
        }
        return jarr;
    }

    private AppdefEntityID getAppdefEntityID(AlertDefinitionValue ad) {
        return new AppdefEntityID(ad.getAppdefType(), ad.getAppdefId());
    }

    /**
     * Create a new escalation.  If alertDefId is non-null, the escalation
     * will also be associated with the given alert definition.
     *
     * @ejb:interface-method
     */
    public Escalation createEscalation(int sessionID, String name, String desc,
                                       boolean allowPause, long maxWaitTime,
                                       boolean notifyAll, boolean repeat,
                                       EscalationAlertType alertType,
                                       Integer alertDefId)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, DuplicateObjectException
    {
        manager.authenticate(sessionID);
        Escalation res;

        // XXX -- We need to do perm-checking here

        res = getEscMan().createEscalation(name, desc, allowPause, maxWaitTime,
                                           notifyAll, repeat);

        if (alertDefId != null) {
            // The alert def needs to use this escalation
            getEscMan().setEscalation(alertType, alertDefId, res);
        }
        return res;
    }

    /**
     * Update basic escalation properties
     *
     * @ejb:interface-method
     */
    public void updateEscalation(int sessionID, Escalation escalation,
                                 String name, String desc, long maxWait,
                                 boolean pausable, boolean notifyAll,
                                 boolean repeat)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, DuplicateObjectException
    {
        AuthzSubject subject = manager.getSubject(sessionID);

        getEscMan().updateEscalation(subject, escalation, name, desc,
                                     pausable, maxWait, notifyAll, repeat);
    }

    /**
     * @ejb:interface-method
     */
    public boolean acknowledgeAlert(int sessionID, EscalationAlertType alertType,
                                 Integer alertID, long pauseWaitTime,
                                 String moreInfo)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, ActionExecuteException
    {
        AuthzSubject subject = manager.getSubject(sessionID);

        return getEscMan().acknowledgeAlert(subject, alertType, alertID,
                                            moreInfo, pauseWaitTime);
    }

    /**
     * Fix a single alert.
     * Method is "NotSupported" since all the alert fixes may take longer
     * than the jboss transaction timeout.  No need for a transaction in this
     * context.
     * @ejb:transaction type="NotSupported"
     * @ejb:interface-method
     */
    public void fixAlert(int sessionID, EscalationAlertType alertType,
                         Integer alertID, String moreInfo)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, ActionExecuteException
    {
        fixAlert(sessionID, alertType, alertID, moreInfo, false);
    }

    /**
     * Fix a batch of alerts.
     * Method is "NotSupported" since all the alert fixes may take longer
     * than the jboss transaction timeout.  No need for a transaction in this
     * context.
     * @ejb:transaction type="NotSupported"
     * @ejb:interface-method
     */
    public void fixAlert(int sessionID, EscalationAlertType alertType,
                         Integer alertID, String moreInfo,
                         boolean fixAllPrevious)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, ActionExecuteException
    {
        AuthzSubject subject = manager.getSubject(sessionID);

        if (fixAllPrevious) {
            long fixCount = fixPreviousAlerts(sessionID, alertType,
                                              alertID, moreInfo);
            if (fixCount > 0) {
                if (moreInfo == null) {
                    moreInfo = "";
                }
                StringBuffer sb = new StringBuffer();
                MessageFormat messageFormat =
                    new MessageFormat(ResourceBundle.getBundle(BUNDLE)
                                        .getString("events.alert.fixAllPrevious"));
                messageFormat.format(
                    new String[] {Long.toString(fixCount)},
                    sb, null);
                moreInfo = sb.toString() + moreInfo;
            }
        }
        // fix the selected alert
        getEscMan().fixAlert(subject, alertType, alertID, moreInfo, false);
    }

    /**
     * Fix all previous alerts.
     * Method is "NotSupported" since all the alert fixes may take longer
     * than the jboss transaction timeout.  No need for a transaction in this
     * context.
     */
    private long fixPreviousAlerts(int sessionID, EscalationAlertType alertType,
                                   Integer alertID, String moreInfo)
        throws SessionTimeoutException, SessionNotFoundException,
               PermissionException, ActionExecuteException
    {
        StopWatch watch = new StopWatch();
        AuthzSubject subject = manager.getSubject(sessionID);
        long fixCount = 0;
        AlertInterface alert = null;
        List alertsToFix = null;

        // Get all previous unfixed alerts.
        watch.markTimeBegin("fixPreviousAlerts: findAlerts");
        if (alertType.equals(ClassicEscalationAlertType.CLASSIC)) {
            alert = getAM().findAlertById(alertID);
            alertsToFix = getAM().findAlerts(
                                       subject.getId(), 0,
                                       alert.getTimestamp(), alert.getTimestamp(),
                                       false, true, null,
                                       alert.getAlertDefinitionInterface().getId(),
                                       PageInfo.getAll(AlertSortField.DATE,
                                                       false));
        } else if (alertType.equals(GalertEscalationAlertType.GALERT)) {
            GalertManagerLocal gMan = GalertManagerEJBImpl.getOne();
            alert = gMan.findAlertLog(alertID);
            alertsToFix = gMan.findAlerts(
                                    subject, AlertSeverity.LOW,
                                    alert.getTimestamp(), alert.getTimestamp(),
                                    false, true, null,
                                    alert.getAlertDefinitionInterface().getId(),
                                    PageInfo.getAll(GalertLogSortField.DATE,
                                                    false));
        } else {
            alertsToFix = Collections.EMPTY_LIST;
        }
        watch.markTimeEnd("fixPreviousAlerts: findAlerts");

        _log.debug("fixPreviousAlerts: alertId = " + alertID
                        + ", previous alerts to fix = " + (alertsToFix.size()-1)
                        + ", time = " + watch);

        try {
            watch.markTimeBegin("fixPreviousAlerts: fixAlert");

            for (Iterator it = alertsToFix.iterator(); it.hasNext(); ) {
                alert = (AlertInterface) it.next();

                try {
                    // Suppress notifications for all previous alerts.
                    if (!alert.getId().equals(alertID)) {
                        getEscMan().fixAlert(subject, alertType, alert.getId(), moreInfo, true);
                        fixCount++;
                    }
                } catch (PermissionException pe) {
                    throw pe;
                } catch (Exception e) {
                    // continue with next alert
                    _log.error("Could not fix alert id "
                                    + alert.getId() + ": " + e.getMessage(), e);
                }
            }
        } finally {
            watch.markTimeEnd("fixPreviousAlerts: fixAlert");
            _log.debug("fixPreviousAlerts: alertId = " + alertID
                            + ", previous alerts fixed = " + fixCount
                            + ", time = " + watch);

            return fixCount;
        }
    }

    /**
     * Get the last fix if available
     * @ejb:interface-method
     */
    public String getLastFix(int sessionID, Integer defId)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException, FinderException {
        AuthzSubject subject = manager.getSubject(sessionID);

        // Look for the last fixed alert
        AlertDefinition def = getADM().getByIdAndCheck(subject, defId);
        return getEscMan().getLastFix(def);
    }

    /**
     * Get a maintenance event by group id
     *
     * @ejb:interface-method
     */
    public MaintenanceEvent getMaintenanceEvent(int sessionId, Integer groupId)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException, SchedulerException
    {
    	AuthzSubject subject = manager.getSubject(sessionId);

    	return getMaintEvtMgr().getMaintenanceEvent(subject, groupId);
    }

    /**
     * Schedule a maintenance event
     *
     * @ejb:interface-method
     */
    public MaintenanceEvent scheduleMaintenanceEvent(int sessionId, MaintenanceEvent event)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException, SchedulerException
    {
    	AuthzSubject subject = manager.getSubject(sessionId);
    	event.setModifiedBy(subject.getName());

    	return getMaintEvtMgr().schedule(subject, event);
    }

    /**
     * Schedule a maintenance event
     *
     * @ejb:interface-method
     */
    public void unscheduleMaintenanceEvent(int sessionId, MaintenanceEvent event)
        throws SessionNotFoundException, SessionTimeoutException,
               PermissionException, SchedulerException
    {
    	AuthzSubject subject = manager.getSubject(sessionId);
    	event.setModifiedBy(subject.getName());

    	getMaintEvtMgr().unschedule(subject, event);
    }

    /**
     * @ejb:interface-method
     */
    public void startup() {
        _log.info("Events Boss starting up!");

        HQApp app = HQApp.getInstance();
        app.registerCallbackListener(DefaultMetricEnableCallback.class,
            new DefaultMetricEnableCallback() {
                public void metricsEnabled(AppdefEntityID ent) {
                    try {
                        _log.info("Inheriting type-based alert defs for " +ent);
                        EventsBossLocal eb = EventsBossEJBImpl.getOne();
                        AuthzSubject hqadmin = AuthzSubjectManagerEJBImpl
                            .getOne().getSubjectById(AuthzConstants.rootSubjectId);
                        eb.inheritResourceTypeAlertDefinition(hqadmin, ent);
                    } catch(Exception e) {
                        throw new SystemException(e);
                    }
                }
            }
        );

        app.registerCallbackListener(ResourceDeleteCallback.class,
            new ResourceDeleteCallback() {
                public void preResourceDelete(Resource r) throws VetoException {
                    AlertDefinitionManagerLocal adm = getADM();
                    adm.disassociateResource(r);
                }
            }
        );

        app.registerCallbackListener(SubjectRemoveCallback.class,
            new SubjectRemoveCallback() {
                public void subjectRemoved(AuthzSubject toDelete) {
                    getEscMan().handleSubjectRemoval(toDelete);
                    getAM().handleSubjectRemoval(toDelete);
                }

            }
        );

        // Add listener to remove alert definition and alerts after resources
        // are deleted.
        HashSet events = new HashSet();
        events.add (ResourceDeletedZevent.class);
        ZeventManager.getInstance().addBufferedListener(events,
            new ZeventListener() {
                public void processEvents(List events) {
                    AlertDefinitionManagerLocal adm = getADM();
                    for (Iterator i = events.iterator(); i.hasNext();) {
                        ResourceZevent z = (ResourceZevent) i.next();
                        if (z instanceof ResourceDeletedZevent) {
                            adm.cleanupAlertDefinitions(z.getAppdefEntityID());
                        }
                    }
                }

                public String toString() {
                    return "AlertDefCleanupListener";
                }
            }
        );
    }

    public static EventsBossLocal getOne() {
        try {
            return EventsBossUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
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
