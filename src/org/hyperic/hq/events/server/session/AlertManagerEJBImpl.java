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

package org.hyperic.hq.events.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hyperic.util.stats.ConcurrentStatsCollector;

import javax.ejb.CreateException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.appdef.shared.AppdefEntityNotFoundException;
import org.hyperic.hq.appdef.shared.AppdefEntityValue;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.shared.ResourceDeletedException;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.common.util.Messenger;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.escalation.server.session.EscalatableCreator;
import org.hyperic.hq.escalation.server.session.EscalationManagerEJBImpl;
import org.hyperic.hq.events.AlertFiredEvent;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.TriggerFiredEvent;
import org.hyperic.hq.events.ext.AbstractTrigger;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertDefinitionManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.shared.EventLogManagerLocal;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEvent;
import org.hyperic.hq.measurement.server.session.AlertConditionsSatisfiedZEventSource;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementDAO;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.stats.ConcurrentStatsCollector;
import org.hyperic.util.timer.StopWatch;

/**
 * @ejb:bean name="AlertManager"
 *      jndi-name="ejb/events/AlertManager"
 *      local-jndi-name="LocalAlertManager"
 *      view-type="local"
 *      type="Stateless"
 *
 * @ejb:transaction type="REQUIRED"
 */
public class AlertManagerEJBImpl extends SessionBase implements SessionBean {
    private final String NOTAVAIL = "Not Available";

    private final Log _log =
        LogFactory.getLog(AlertManagerEJBImpl.class.getName());
    private final String VALUE_PROCESSOR =
        PagerProcessor_events.class.getName();

    private Pager valuePager;
    private Pager pojoPager;
    private EventLogManagerLocal eventLogManager;

    public AlertManagerEJBImpl() {}

    private AlertDefinitionDAO getAlertDefDAO() {
        return new AlertDefinitionDAO(DAOFactory.getDAOFactory());
    }

    private AlertDAO getAlertDAO() {
        return new AlertDAO(DAOFactory.getDAOFactory());
    }

    private AlertConditionDAO getAlertConDAO() {
        return new AlertConditionDAO(DAOFactory.getDAOFactory());
    }

    /**
     * Create a new alert.
     *
     * @param def The alert definition.
     * @param ctime The alert creation time.
     * @ejb:interface-method
     */
    public Alert createAlert(AlertDefinition def, long ctime) {
        Alert alert = new Alert();
        alert.setAlertDefinition(def);
        alert.setCtime(ctime);
        getAlertDAO().save(alert);
        return alert;
    }

    /**
     * Simply mark an alert object as fixed
     *
     * @ejb:interface-method
     */
    public void setAlertFixed(Alert alert) {
        alert.setFixed(true);

        // If the alert definition is set to "recover", then we should enable it.
        AlertDefinition def = alert.getAlertDefinition();

        if (def.isWillRecover()) {
            try {
                AlertDefinitionManagerEJBImpl.getOne().updateAlertDefinitionInternalEnable( AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo(), def, true);
            } catch (PermissionException e) {
                _log.error("Error re-enabling alert with ID: " + def.getId() + " after it was fixed.",e);
            }
        }
    }

    /**
     * Log the details of an action's execution
     *
     * @ejb:interface-method
     */
    public void logActionDetail(Alert alert, Action action, String detail,
                                AuthzSubject subject)
    {
        alert.createActionLog(detail, action, subject);
    }

    public void addConditionLogs(Alert alert, AlertConditionLogValue[] logs) {
        AlertConditionDAO dao = getAlertConDAO();
        for (int i = 0; i < logs.length; i++) {
            AlertCondition cond = dao.findById(logs[i].getCondition().getId());
            alert.createConditionLog(logs[i].getValue(), cond);
        }
    }

    /** Remove alerts
     * @ejb:interface-method
     */
    public void deleteAlerts(Integer[] ids) {
        getAlertDAO().deleteByIds(ids);
    }

    /**
     * Remove alerts for an appdef entity
     * @throws PermissionException
     * @ejb:interface-method
     */
    public int deleteAlerts(AuthzSubject subj, AppdefEntityID id)
        throws PermissionException {
        // ...check that user has delete permission on alert definition's resource...
        canDeleteAlertDefinition(subj, id);

        return getAlertDAO().deleteByResource(findResource(id));
    }

    /**
     * Remove alerts for an alert definition
     * @throws PermissionException
     * @ejb:interface-method
     */
    public int deleteAlerts(AuthzSubject subj, AlertDefinition ad)
        throws RemoveException, PermissionException {
        // ...check that user has delete permission on alert definition's resource...
        canDeleteAlertDefinition(subj, ad.getAppdefEntityId());

        return getAlertDAO().deleteByAlertDefinition(ad);
    }

    /**
     * Remove alerts for a range of time
     * @ejb:interface-method
     */
    public int deleteAlerts(long begin, long end) {
        return getAlertDAO().deleteByCreateTime(begin, end);
    }

    /**
     * Find an alert by ID
     *
     * @ejb:interface-method
     */
    public AlertValue getById(Integer id) {
        return (AlertValue) valuePager.processOne(getAlertDAO().get(id));
    }

    /**
     * @ejb:interface-method
     */
    public Alert getAlertById(Integer id) {
        return getAlertDAO().getById(id);
    }

    /**
     * Find an alert pojo by ID
     *
     * @ejb:interface-method
     */
    public Alert findAlertById(Integer id) {
        Alert alert = getAlertDAO().findById(id);
        Hibernate.initialize(alert);

        alert.setAckable(EscalationManagerEJBImpl.getOne()
                         .isAlertAcknowledgeable(alert.getId(),
                                                 alert.getDefinition()));

        return alert;
    }

    /**
     * Find the last unfixed alert by definition ID
     *
     * @ejb:interface-method
     */
    public Alert findLastUnfixedByDefinition(AuthzSubject subj, Integer id)
    {
        try {
            AlertDefinition def = getAlertDefDAO().findById(id);
            return getAlertDAO().findLastByDefinition(def, false);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Find all last unfixed alerts
     *
     * @ejb:interface-method
     */
    public Map findAllLastUnfixed() {        
        StopWatch watch = new StopWatch();
        Map unfixedAlerts = null;
        try {
            unfixedAlerts = 
                getAlertDAO().findAllLastUnfixed();
        } catch (Exception e) {
            unfixedAlerts = Collections.EMPTY_MAP;
            _log.error("Error finding all last unfixed alerts", e);
        } finally {
            if (_log.isDebugEnabled()) {
                _log.debug("findAllLastUnfixed: " + watch);
            }
        }
        
        return unfixedAlerts;
    }
    
    /**
     * Find the last alerts for the given resource
     *
     * @ejb:interface-method
     */
    public Map findLastByResource(AuthzSubject subj, 
                                  Resource r,
                                  boolean includeDescendants,
                                  boolean fixed) {
        
        StopWatch watch = new StopWatch();
        Map unfixedAlerts = null;
        try {
            unfixedAlerts = 
                getAlertDAO().findLastByResource(subj, r, includeDescendants, fixed);
        } catch (Exception e) {
            unfixedAlerts = Collections.EMPTY_MAP;
            _log.error("Error finding the last alerts for resource id=" + r.getId(), e);
        } finally {
            if (_log.isDebugEnabled()) {
                _log.debug("findLastByResource: " + watch);
            }
        }
        
        return unfixedAlerts;
    }
    
    /**
     * Find the last alert by definition ID
     * @throws PermissionException
     *
     * @ejb:interface-method
     */
    public Alert findLastByDefinition(Integer id) {
        try {
            AlertDefinition def = getAlertDefDAO().findById(id);
            return getAlertDAO().findLastByDefinition(def);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Find the last alert by definition ID
     * @throws PermissionException
     *
     * @ejb:interface-method
     */
    public Alert findLastFixedByDefinition(AlertDefinition def) {
        try {
            return getAlertDAO().findLastByDefinition(def, true);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the # of alerts within HQ inventory
     * @ejb:interface-method
     */
    public Number getAlertCount() {
        return new Integer(getAlertDAO().size());
    }

    /**
     * Get the number of alerts for the given array of AppdefEntityID's
     * @ejb:interface-method
     */
    public int[] getAlertCount(AppdefEntityID[] ids) {
        AlertDAO dao = getAlertDAO();
        int[] counts = new int[ids.length];
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].isPlatform() || ids[i].isServer() || ids[i].isService()){
                counts[i] = dao.countAlerts(findResource(ids[i])).intValue();
            }
        }
        return counts;
    }

    /**
     * Processes {@link AlertConditionSatisfiedZEvent} that indicate that an alert should be created
     * 
     * To minimize StaleStateExceptions, this method should only be called once in one transaction.
     *      
     * @ejb:interface-method
     */
    public void fireAlert(AlertConditionsSatisfiedZEvent event) {
        if (!AlertRegulator.getInstance().alertsAllowed()) {
            _log.debug("Alert not firing because they are not allowed");
            return;
        }
        long startTime = System.currentTimeMillis();
        try {
            AlertDefinitionManagerLocal aman =
                AlertDefinitionManagerEJBImpl.getOne();

            Integer adId = Integer.valueOf(((AlertConditionsSatisfiedZEventSource)event.getSourceId()).getId());

            AlertDefinition alertDef = null;

            //Check persisted alert def status
            if (!aman.isEnabled(adId)) {
                  return;
            }

           alertDef = aman.getByIdNoCheck(adId);


           if (alertDef.getFrequencyType() == EventConstants.FREQ_ONCE ||
                    alertDef.isWillRecover()) {
                // Disable the alert definition now that we've fired
                aman.updateAlertDefinitionInternalEnable(
                            AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo(),
                            alertDef,
                            false);
            }

            EscalatableCreator creator =
                new ClassicEscalatableCreator(alertDef, event, new Messenger(), AlertManagerEJBImpl.getOne());
            Resource res = creator.getAlertDefinition().getResource();
            if (res == null || res.isInAsyncDeleteState()) {
                return;
            }

            // Now start escalation
            if (alertDef.getEscalation() != null) {
                EscalationManagerEJBImpl.getOne()
                                    .startEscalation(alertDef, creator);
            } else {
                creator.createEscalatable();
            }
            
            if (_log.isDebugEnabled()) {
                _log.debug("Alert definition " + alertDef.getName() +
                           " (id=" + alertDef.getId() + ") fired.");
            }
            
            ConcurrentStatsCollector.getInstance().addStat(System.currentTimeMillis() - startTime,ConcurrentStatsCollector.FIRE_ALERT_TIME);
        } catch (PermissionException e) {
            _log.error("Alert not firing due to a permissions issue",e);
        } catch (ResourceDeletedException e) {
            _log.debug(e,e);
        }
    }

    /**
     * Get a collection of all alerts
     *
     * @ejb:interface-method
     */
    public PageList findAllAlerts() {
        // TODO - This is messy, from what I can tell this collection contains
        //        a list of both alert POJOs and AlertValue objects.  Why this
        //        is done, I haven't figured out yet, just adding a note so I
        //        don't forget to come back to it.
        Collection res;

        res = getAlertDAO().findAll();
        for (Iterator i = getAlertDAO().findAll().iterator(); i.hasNext();) {
            Alert alert = (Alert) i.next();

            res.add(alert.getAlertValue());
        }

        return new PageList(res, res.size());
    }

    /**
     * Get a collection of alerts for an AppdefEntityID
     * @throws PermissionException
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(AuthzSubject subj, AppdefEntityID id,
                               PageControl pc)
        throws PermissionException {
        // ...check that user has view permission on alert definition's resource...
        canViewAlertDefinition(subj, id);

        Resource resource = findResource(id);
        List alerts;

        if (pc.getSortattribute() == SortAttribute.NAME) {
            alerts = getAlertDAO().findByResourceSortByAlertDef(resource);
        } else {
            alerts = getAlertDAO().findByResource(resource);
        }

        if (pc.getSortorder() == PageControl.SORT_DESC)
            Collections.reverse(alerts);

        return valuePager.seek(alerts, pc);
    }

    /**
     * Get a collection of alerts for an AppdefEntityID and time range
     * @throws PermissionException
     *
     * @ejb:interface-method
     */
    public PageList findAlerts(AuthzSubject subj, AppdefEntityID id,
                               long begin, long end, PageControl pc)
        throws PermissionException
    {
        
        // ...check that user has view permission on alert definition's resource...
        canViewAlertDefinition(subj, id);

        Resource resource = findResource(id);
        List alerts =
            getAlertDAO().findByAppdefEntityInRange(resource,
                                                    begin, end,
                                                    pc.getSortattribute() == SortAttribute.NAME,
                                                    pc.isAscending());

        return pojoPager.seek(alerts, pc);
    }

    /**
     * A more optimized look up which includes the permission checking
     * @ejb:interface-method
     */
    public List findAlerts(Integer subj, int priority, long timeRange,
                           long endTime, boolean inEsc, boolean notFixed,
                           Integer groupId, PageInfo pageInfo)
        throws PermissionException
    {
        return findAlerts(subj, priority, timeRange, endTime,
                          inEsc, notFixed, groupId, null, pageInfo);
    }

    /**
     * A more optimized look up which includes the permission checking
     * @return {@link List} of {@link Alert}s
     * @ejb:interface-method
     */
    public List findAlerts(Integer subj, int priority, long timeRange,
                           long endTime, boolean inEsc, boolean notFixed,
                           Integer groupId, Integer alertDefId,
                           PageInfo pageInfo)
        throws PermissionException
    {
        // [HHQ-2946] Only round up if end time is not a multiple of a minute
        long mod = endTime % 60000;
        if (mod > 0) {
            // Time voodoo the end time to the nearest minute so that we might
            // be able to use cached results.
            endTime = TimingVoodoo.roundUpTime(endTime, 60000);
        }
        return getAlertDAO().findByCreateTimeAndPriority(subj,
                                                         endTime - timeRange,
                                                         endTime, priority,
                                                         inEsc, notFixed,
                                                         groupId, alertDefId,
                                                         pageInfo);
    }

    /**
     * Search alerts given a set of criteria
     *
     * @param timeRange
     *            the amount of milliseconds prior to current that the alerts
     *            will be contained in. e.g. the beginning of the time range
     *            will be (current - timeRante)
     * @param page
     *            TODO
     *
     * @param includes {@link List} of {@link AppdefEntityID}s to filter,
     *  may be null for all.
     * @ejb:interface-method
     */
    public List findAlerts(AuthzSubject subj, int count, int priority,
                           long timeRange, long endTime, List includes)
        throws PermissionException
    {
        List result = new ArrayList();
        final Set inclSet = (includes == null) ? null : new HashSet(includes);

        for (int index = 0; result.size() < count; index++) {
            // Permission checking included
            PageInfo pInfo = PageInfo.create(index, count, AlertSortField.DATE,
                                             false);
            // XXX need to change this to pass in specific includes so that
            // the session does not blow up with too many objects
            List alerts = findAlerts(subj.getId(), priority, timeRange,
                                     endTime, false, false, null, pInfo);
            if (alerts.size() == 0) {
                break;
            }
            if (inclSet != null) {
                Iterator it = alerts.iterator();
                for (int i = 0; it.hasNext(); i++) {
                    Alert alert = (Alert) it.next();
                    AlertDefinition alertdef = alert.getAlertDefinition();

                    // Filter by appdef entity
                    AppdefEntityID aeid = alertdef.getAppdefEntityId();
                    if (!inclSet.contains(aeid)) {
                        continue;
                    }

                    // Add it
                    result.add(alert);

                    // Finished
                    if (result.size() == count) {
                        break;
                    }
                }
            }
            else {
                return alerts;
            }
        }

        return result;
    }

    /**
     * Find escalatables for a resource in a given time range.
     *
     * @see findAlerts(AuthzSubject, int, int, long, long, List)
     *
     * @ejb:interface-method
     */
    public List findEscalatables(AuthzSubject subj, int count,
                                 int priority, long timeRange, long endTime,
                                 List includes)
        throws PermissionException
    {
        List alerts = findAlerts(subj, count, priority, timeRange, endTime,
                                 includes);
        return convertAlertsToEscalatables(alerts);
    }

    /**
     * A more optimized look up which includes the permission checking
     * @ejb:interface-method
     */
    public int getUnfixedCount(Integer subj, long timeRange, long endTime,
                               Integer groupId)
        throws PermissionException
    {
        // Time voodoo the end time to the nearest minute so that we might
        // be able to use cached results
        endTime = TimingVoodoo.roundUpTime(endTime, 60000);
        Integer count = getAlertDAO().countByCreateTimeAndPriority(subj,
                                                         endTime - timeRange,
                                                         endTime, 0,
                                                         false, true,
                                                         groupId, null);
        if (count != null)
            return count.intValue();

        return 0;
    }

    private List convertAlertsToEscalatables(Collection alerts) {
        List res = new ArrayList(alerts.size());

        for (Iterator i=alerts.iterator(); i.hasNext(); ) {
            Alert a = (Alert)i.next();
            // due to async deletes this could be null.  just ignore and continue
            if (a.getAlertDefinition().getResource().isInAsyncDeleteState()) {
                continue;
            }
            Escalatable e =
                ClassicEscalatableCreator.createEscalatable(a,
                                                            getShortReason(a),
                                                            getLongReason(a));
            res.add(e);
        }
        return res;
    }

    /**
     * Get the long reason for an alert
     * @ejb:interface-method
     */
    public String getShortReason(Alert alert) {
        AlertDefinition def = alert.getAlertDefinition();
        Resource r = def.getResource();
        if (r == null || r.isInAsyncDeleteState()) {
            return "alertid=" + alert.getId() + " is associated with an invalid or deleted resource";
        }
        AppdefEntityID aeid = new AppdefEntityID(r);
        AppdefEntityValue aev = new AppdefEntityValue(
            aeid, AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo());

        String name = "";

        try {
            name = aev.getName();
        } catch (AppdefEntityNotFoundException e) {
            log.warn("Alert short reason requested for invalid resource " +
                     aeid);
        } catch (PermissionException e) {
            // Should never happen
            log.error("Overlord does not have permission for resource " + aeid);
        }

        // Get the alert definition's conditions
        Collection clogs = alert.getConditionLog();

        StringBuffer text =
            new StringBuffer(def.getName())
                .append(" ")
                .append(name)
                .append(" ");

        MeasurementDAO dmDao =
            new MeasurementDAO(DAOFactory.getDAOFactory());
        for (Iterator it = clogs.iterator(); it.hasNext(); ) {
            AlertConditionLog log = (AlertConditionLog) it.next();
            AlertCondition cond = log.getCondition();

            Measurement dm;

            switch (cond.getType()) {
            case EventConstants.TYPE_THRESHOLD:
            case EventConstants.TYPE_BASELINE:
                dm = dmDao.findById(new Integer(cond.getMeasurementId()));
                // Value is already formatted by HHQ-2573
                String actualValue = log.getValue();

                text.append(cond.getName())
                    .append(" (").append(actualValue).append(") ");
                break;
            case EventConstants.TYPE_CONTROL:
                text.append(cond.getName());
                break;
            case EventConstants.TYPE_CHANGE:
                dm = dmDao.findById(new Integer(cond.getMeasurementId()));
                text.append(cond.getName())
                    .append(" (")
                    .append(log.getValue())
                    .append(") ");
                break;
            case EventConstants.TYPE_CUST_PROP:
                text.append(cond.getName()).append(" (")
                    .append(log.getValue()).append(") ");
                break;
            case EventConstants.TYPE_LOG:
                text.append("Log (")
                    .append(log.getValue())
                    .append(") ");
                break;
            case EventConstants.TYPE_CFG_CHG:
                text.append("Config changed (")
                    .append(log.getValue())
                    .append(") ");
                break;
            default:
                break;
            }
        }

        // Get the short reason for the alert
        return text.toString();
    }

    /**
     * Get the long reason for an alert
     * @ejb:interface-method
     */
    public String getLongReason(Alert alert) {
        final String indent = "    ";

        // Get the alert definition's conditions
        Collection clogs = alert.getConditionLog();

        AlertConditionLog[] logs = (AlertConditionLog[])
            clogs.toArray(new AlertConditionLog[clogs.size()]);

        StringBuffer text = new StringBuffer();
        MeasurementDAO dmDao =
            new MeasurementDAO(DAOFactory.getDAOFactory());
        for (int i = 0; i < logs.length; i++) {
            AlertCondition cond = logs[i].getCondition();

            if (i == 0) {
                text.append("\n").append(indent).append("If ");
            }
            else {
                text.append("\n").append(indent)
                    .append(cond.isRequired() ? "AND " : "OR ");
            }

//            TriggerFiredEvent event = (TriggerFiredEvent)
//            eventMap.get( cond.getTriggerId() );

            Measurement dm = null;

            switch (cond.getType()) {
            case EventConstants.TYPE_THRESHOLD:
            case EventConstants.TYPE_BASELINE:
                dm = dmDao.findById(new Integer(cond.getMeasurementId()));
                text.append(describeCondition(cond, dm));

                // Value is already formatted by HHQ-2573
                String actualValue = logs[i].getValue();
                text.append(" (actual value = ").append(actualValue).append(")");
                break;
            case EventConstants.TYPE_CONTROL:
                text.append(describeCondition(cond, dm));
                break;
            case EventConstants.TYPE_CHANGE:
                text.append(describeCondition(cond, dm))
                    .append(" (New value: ")
                    .append(logs[i].getValue())
                    .append(")");
                break;
            case EventConstants.TYPE_CUST_PROP:
                text.append(describeCondition(cond, dm))
                    .append("\n").append(indent).append(logs[i].getValue());
                break;
            case EventConstants.TYPE_LOG:
                text.append(describeCondition(cond, dm))
                    .append("\n").append(indent).append("Log: ")
                    .append(logs[i].getValue());
                break;
            case EventConstants.TYPE_CFG_CHG:
                text.append(describeCondition(cond, dm))
                    .append("\n").append(indent).append("Details: ")
                    .append(logs[i].getValue());
                break;
            default:
                break;
            }
        }

        return text.toString();
    }

    /**
     * @ejb:interface-method
     */
    public void handleSubjectRemoval(AuthzSubject subject) {
        AlertActionLogDAO dao =
            new AlertActionLogDAO(DAOFactory.getDAOFactory());
        dao.handleSubjectRemoval(subject);
    }

    public static AlertManagerLocal getOne() {
        try {
            return AlertManagerUtil.getLocalHome().create();
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }

    void setEventLogManager(EventLogManagerLocal eventLogManager) {
        this.eventLogManager = eventLogManager;
    }

    public void ejbCreate() throws CreateException {
        try {
        	// We need to phase out the Value objects...
            valuePager = Pager.getPager(VALUE_PROCESSOR);

            // ...and start using the POJOs instead
            pojoPager = Pager.getDefaultPager();
        } catch ( Exception e ) {
            throw new CreateException("Could not create value pager:" + e);
        }
        this.eventLogManager = EventLogManagerEJBImpl.getOne();
    }

    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
