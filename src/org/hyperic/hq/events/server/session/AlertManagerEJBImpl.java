/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2007], Hyperic, Inc.
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.server.trigger.conditional.ValueChangeTrigger;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.escalation.server.session.Escalatable;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.events.server.session.AlertSortField;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.measurement.TimingVoodoo;
import org.hyperic.hq.measurement.UnitsConvert;
import org.hyperic.hq.measurement.server.session.DerivedMeasurement;
import org.hyperic.hq.measurement.server.session.DerivedMeasurementDAO;
import org.hyperic.hq.measurement.shared.ResourceLogEvent;
import org.hyperic.util.NumberUtil;
import org.hyperic.util.pager.PageControl;
import org.hyperic.util.pager.PageList;
import org.hyperic.util.pager.Pager;
import org.hyperic.util.pager.SortAttribute;
import org.hyperic.util.units.FormattedNumber;

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

    private Pager          valuePager;
    
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
     * Returns true if alerts are allowed.
     * @ejb:interface-method
     */
    public boolean alertsAllowed() {
        return AlertRegulator.getInstance().alertsAllowed();
    }
    
    /**
     * @see alertsAllowed()
     * @ejb:interface-method
     */
    public void setAlertsAllowed(boolean allowed) {
        AlertRegulator.getInstance().setAlertsAllowed(allowed);
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
        
        if (def.isWillRecover() && !def.isEnabled()) {
            def.setEnabledStatus(true);
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
    public int deleteAlerts(AuthzSubjectValue subj, AppdefEntityID id)
        throws PermissionException {
        canManageAlerts(subj.getId(), id);
        return getAlertDAO().deleteByEntity(id);
    }

    /** 
     * Remove alerts for an alert definition
     * @throws PermissionException 
     * @ejb:interface-method
     */
    public int deleteAlerts(AuthzSubjectValue subj, AlertDefinition ad)
        throws RemoveException, PermissionException {
        canManageAlerts(subj, ad);
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
        return (AlertValue) valuePager.processOne(getAlertDAO().findById(id));
    }

    /**
     * Find an alert pojo by ID
     *
     * @ejb:interface-method
     */
    public Alert findAlertById(Integer id) {
        Alert alert = getAlertDAO().findById(id);
        Hibernate.initialize(alert);
        return alert;
    }

    /**
     * Find the last alert by definition ID
     * @throws PermissionException 
     * 
     * @ejb:interface-method
     */
    public Alert findLastUnfixedByDefinition(AuthzSubjectValue subj, Integer id)
    {
        try {
            AlertDefinition def = getAlertDefDAO().findById(id);
            return getAlertDAO().findLastByDefinition(def, false);
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
     * Get the number of alerts for the given array of AppdefEntityID's
     * @ejb:interface-method
     */
    public int[] getAlertCount(AppdefEntityID[] ids) {
        AlertDAO dao = getAlertDAO();
        int[] counts = new int[ids.length];
        for (int i = 0; i < ids.length; i++) {
            if (ids[i].isPlatform() || ids[i].isServer() || ids[i].isService()){
                counts[i] = dao.countAlerts(ids[i]).intValue();
            }
        }
        return counts;
    }

    /**
     * Get a collection of all alerts
     *
     * @ejb:interface-method
     */
    public PageList findAllAlerts() {
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
    public PageList findAlerts(AuthzSubjectValue subj, AppdefEntityID id,
                               PageControl pc)
        throws PermissionException {
        canManageAlerts(subj.getId(), id);
        List alerts;

        if (pc.getSortattribute() == SortAttribute.NAME) {
            alerts = getAlertDAO().findByAppdefEntitySortByAlertDef(id);
        } else {
            alerts = getAlertDAO().findByEntity(id);
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
    public PageList findAlerts(AuthzSubjectValue subj, AppdefEntityID id,
                               long begin, long end, PageControl pc)
        throws PermissionException 
    {
        canManageAlerts(subj.getId(), id);
        List alerts = getAlertDAO().findByAppdefEntityInRange(id, begin, end,
                               pc.getSortattribute() == SortAttribute.NAME,
                               pc.isAscending());

        return valuePager.seek(alerts, pc);
    }

    /**
     * A more optimized look up which includes the permission checking
     * @ejb:interface-method
     */
    public List findAlerts(Integer subj, int priority, long timeRange,
                           long endTime, PageInfo pageInfo) 
        throws PermissionException 
    {
        // Time voodoo the end time to the nearest minute so that we might
        // be able to use cached results
        endTime = TimingVoodoo.roundUpTime(endTime, 60000);
        return getAlertDAO().findByCreateTimeAndPriority(subj,
                                                         endTime- timeRange,
                                                         endTime, priority,
                                                         pageInfo);
    }
    
    /**
     * Search alerts given a set of criteria
     * @param timeRange the amount of milliseconds prior to current that the
     *                  alerts will be contained in.  e.g. the beginning of the  
     *                  time range will be (current - timeRante)
     * @param page TODO
     *
     * @ejb:interface-method
     */
    public List findAlerts(AuthzSubjectValue subj, int count, int priority,
                           long timeRange, long endTime, List includes) 
        throws PermissionException 
    {
        List result = new ArrayList();
        
        for (int index = 0; result.size() < count; index++) {
            // Permission checking included
            PageInfo pInfo = PageInfo.create(index, count, AlertSortField.DATE,
                                             false);
            List alerts = findAlerts(subj.getId(), priority, timeRange,
                                     endTime, pInfo);
            if (alerts.size() == 0)
                break;
            
            if (includes != null) {
                Iterator it = alerts.iterator();
                for (int i = 0; it.hasNext(); i++) {
                    Alert alert = (Alert) it.next();
                    AlertDefinition alertdef = alert.getAlertDefinition();

                    // Filter by appdef entity
                    AppdefEntityID aeid = alertdef.getAppdefEntityId();
                    if (!includes.contains(aeid))
                        continue;

                    // Add it
                    result.add(alert);
                    
                    // Finished
                    if (result.size() == count)
                        break;
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
     * @see findAlerts(AuthzSubjectValue, int, int, long, long, List)
     *
     * @ejb:interface-method
     */
    public List findEscalatables(AuthzSubjectValue subj, int count, 
                                 int priority, long timeRange, long endTime, 
                                 List includes)
        throws PermissionException
    {
        List alerts = findAlerts(subj, count, priority, timeRange, endTime,
                                 includes);
        return convertAlertsToEscalatables(alerts);
    }

    private List convertAlertsToEscalatables(Collection alerts) {
        List res = new ArrayList(alerts.size());

        for (Iterator i=alerts.iterator(); i.hasNext(); ) {
            Alert a = (Alert)i.next();
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
        AppdefEntityID aeid =
            new AppdefEntityID(def.getAppdefType(), def.getAppdefId());
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
        
        // Get the short reason for the alert
        return def.getName() + " " + name;
    }
    
    /**
     * Convert the alert condition log value into a number in the units specified 
     * by the derived measurement.
     * 
     * @param log The alert condition log.
     * @param dm The derived measurement.
     * @return The string representation of the converted alert condition log 
     *         value or <code>NOTAVAIL</code> if the value cannot be converted.
     */
    private String safeGetAlertConditionLogNumericValue(AlertConditionLog log, 
                                                        DerivedMeasurement dm) {
        Number val = NumberUtil.stringAsNumber(log.getValue());
        
        if (NumberUtil.NaN.equals(val)) {
            _log.warn("Alert condition log with id="+log.getId()+" has value that " +
            		"cannot be converted to a number: "+log.getValue());
            return NOTAVAIL;
        } else {
            FormattedNumber av = UnitsConvert.convert(
                    val.doubleValue(), dm.getTemplate().getUnits());
            return av.toString();            
        }
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

            DerivedMeasurementDAO dmDao =
                new DerivedMeasurementDAO(DAOFactory.getDAOFactory());
            DerivedMeasurement dm;
            
            switch (cond.getType()) {
            case EventConstants.TYPE_THRESHOLD:
            case EventConstants.TYPE_BASELINE:
                text.append(cond.getName()).append(" ")
                    .append(cond.getComparator()).append(" ");

                dm = dmDao.findById(new Integer(cond.getMeasurementId()));

                if (cond.getType() == EventConstants.TYPE_BASELINE) {
                    text.append(cond.getThreshold());
                    text.append("% of ");

                    if (MeasurementConstants.BASELINE_OPT_MAX.equals(cond
                            .getOptionStatus())) {
                        text.append("Max Value");
                    } else if (MeasurementConstants.BASELINE_OPT_MIN
                            .equals(cond.getOptionStatus())) {
                        text.append("Min Value");
                    } else {
                        text.append("Baseline");
                    }
                } else {
                    FormattedNumber th =
                        UnitsConvert.convert(cond.getThreshold(),
                                             dm.getTemplate().getUnits());
                    text.append(th.toString());
                }

                // Format the number
                String actualValue = safeGetAlertConditionLogNumericValue(logs[i], dm);
                text.append(" (actual value = ").append(actualValue).append(")");
                break;
            case EventConstants.TYPE_CONTROL:
                text.append(cond.getName());
                break;
            case EventConstants.TYPE_CHANGE:
                dm = dmDao.findById(new Integer(cond.getMeasurementId()));
                text.append(cond.getName()).append(" value changed");
                // Parse out old value. This is a hack.
                // Basically, we use the MessageFormat from the
                // ValueChangeTrigger class to parse out the
                // arguments from the event's message which was
                // created from the same message format. This is
                // the best we can do until we track previous
                // values more explicitly. (JW)
                text.append(" (");
                try {
                    Object[] values = ValueChangeTrigger.MESSAGE_FMT
                            .parse(logs[i].getValue());
                    text.append("old value = ");
                    if (log.isTraceEnabled()) {
                        log.trace("event message = " + logs[i].getValue());
                        for (int x = 0; x < values.length; ++x) {
                            log.trace("values[" + x + "] = " + values[x]);
                        }
                    }
                    if (2 == values.length) {
                        text.append(values[1]);
                    } else {
                        text.append(NOTAVAIL);
                    }
                } catch (ParseException e) {
                    text.append(NOTAVAIL);
                }
                
                String newValue = safeGetAlertConditionLogNumericValue(logs[i], dm);
                text.append(", new value = ").append(newValue).append(")");
                break;
            case EventConstants.TYPE_CUST_PROP:
                text.append(cond.getName()).append(" value changed");
                text.append("\n").append(indent).append(logs[i].getValue());
                break;
            case EventConstants.TYPE_LOG:
                text.append("Event/Log Level(")
                        .append(ResourceLogEvent.getLevelString(Integer
                                        .parseInt(cond.getName())))
                        .append(")");
                if (cond.getOptionStatus() != null
                        && cond.getOptionStatus().length() > 0) {
                    text.append(" and matching substring ").append('"')
                        .append(cond.getOptionStatus()).append('"');
                }

                text.append("\n").append(indent).append("Log: ")
                        .append(logs[i].getValue());
                break;
            case EventConstants.TYPE_CFG_CHG:
                text.append("Config changed");
                if (cond.getOptionStatus() != null
                        && cond.getOptionStatus().length() > 0) {
                    text.append(": ")
                        .append(cond.getOptionStatus());
                }

                text.append("\n").append(indent).append("Details: ")
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
    
    public void ejbCreate() throws CreateException {
        try {
            valuePager = Pager.getPager(VALUE_PROCESSOR);
        } catch ( Exception e ) {
            throw new CreateException("Could not create value pager:" + e);
        }
    }

    public void ejbRemove() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void setSessionContext(SessionContext ctx) {}
}
