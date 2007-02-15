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
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.shared.AuthzSubjectValue;
import org.hyperic.hq.authz.shared.PermissionException;
import org.hyperic.hq.bizapp.server.trigger.conditional.ValueChangeTrigger;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.EventConstants;
import org.hyperic.hq.events.shared.AlertActionLogValue;
import org.hyperic.hq.events.shared.AlertConditionLogValue;
import org.hyperic.hq.events.shared.AlertManagerLocal;
import org.hyperic.hq.events.shared.AlertManagerUtil;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.hq.events.server.session.Action;
import org.hyperic.hq.events.server.session.Alert;
import org.hyperic.hq.measurement.MeasurementConstants;
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
 * The alert manager.
 *
 * @ejb:bean name="AlertManager"
 *      jndi-name="ejb/events/AlertManager"
 *      local-jndi-name="LocalAlertManager"
 *      view-type="local"
 *      type="Stateless"
 * 
 * @ejb:transaction type="REQUIRED"
 */
public class AlertManagerEJBImpl extends SessionBase implements SessionBean {
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

    private ActionDAO getActionDAO() {
        return new ActionDAO(DAOFactory.getDAOFactory());
    }
    
    /**
     * Create a new alert
     *
     * @ejb:interface-method
     */
    public AlertValue createAlert(AlertValue val) {
        AlertDefinition def = getAlertDefDAO().findById(val.getAlertDefId());
        Alert alert = def.createAlert(val);
        getAlertDAO().save(alert);
        return alert.getAlertValue();
    }

    /**
     * Simply mark an alert object as fixed
     *
     * @ejb:interface-method
     */
    public void setAlertFixed(Alert alert) {
        alert.setFixed(true);
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

    /**
     * Update the alert
     *
     * @ejb:interface-method
     */
    public Alert updateAlert(AlertValue val) {
        Alert alert;
        
        alert = getAlertDAO().findById(val.getId());

        // Go through the AlertConditionLogs and create them
        for (Iterator i = val.getAddedConditionLogs().iterator();
             i.hasNext();)
        {
            AlertConditionLogValue aclv = (AlertConditionLogValue) i.next();
            AlertCondition cond =
                getAlertConDAO().findById(aclv.getCondition().getId());
            
            AlertConditionLog log =
                alert.createConditionLog(aclv.getValue(), cond);
            DAOFactory.getDAOFactory().getAlertConditionLogDAO().save(log);
        }
            
        // Go through the AlertActionLogs and create them
        Collection alogs = val.getAddedActionLogs();
        for (Iterator i = alogs.iterator(); i.hasNext(); ) {
            AlertActionLogValue aalv = (AlertActionLogValue) i.next();
            Action action = getActionDAO().findById(aalv.getActionId());
                
            AlertActionLog log = alert.createActionLog(aalv.getDetail(),
                                                       action, null);
            DAOFactory.getDAOFactory().getAlertActionLogDAO().save(log);
        }
        return alert;
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
        canManageAlerts(subj, id);
        return getAlertDAO().deleteByEntity(id);
    }

    /** 
     * Remove alerts for an alert definition
     * @throws PermissionException 
     * @ejb:interface-method
     */
    public int deleteAlerts(AuthzSubjectValue subj, Integer defId)
        throws RemoveException, PermissionException {
        AlertDefinition ad = getAlertDefDAO().findById(defId);
        canManageAlerts(subj, ad);
        return getAlertDAO().deleteByAlertDefinition(defId);
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
     * Find an alert by definition ID and time
     * @throws PermissionException 
     * 
     * @ejb:interface-method
     */
    public AlertValue getByAlertDefAndTime(AuthzSubjectValue subj, Integer id,
                                           long ctime)
        throws PermissionException {
        AlertDAO aDao = getAlertDAO();
        AlertDefinition def = getAlertDefDAO().findById(id);
        canManageAlerts(subj, def);
        return aDao.findByAlertDefinitionAndCtime(def, ctime).getAlertValue(); 
    }


    /**
     * Find the last alert by definition ID
     * @throws PermissionException 
     * 
     * @ejb:interface-method
     */
    public Alert findLastUnfixedByDefinition(AuthzSubjectValue subj, Integer id)
        throws PermissionException {
        AlertDefinition def = getAlertDefDAO().findById(id);
        canManageAlerts(subj, def);
        
        return getAlertDAO().findLastUnfixedByDefinition(def);
    }

    /**
     * Get a the number of alerts for a given alert definition
     *
     * @ejb:interface-method
     */
    public int getAlertCount(Integer alertDefId) {
        AlertDefinition def = getAlertDefDAO().findById(alertDefId);
    
        return getAlertDAO().countAlerts(def).intValue();
    }

    /**
     * Get the number of alerts for the given array of AppdefEntityID's
     * @ejb:interface-method
     */
    public int[] getAlertCount(AppdefEntityID[] ids) {
        AlertDAO dao = getAlertDAO();
        int[] counts = new int[ids.length];
        for (int i = 0; i < ids.length; i++) {
            counts[i] = dao.countAlerts(ids[i]).intValue();
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
        canManageAlerts(subj, id);
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
        canManageAlerts(subj, id);
        AlertDAO aDao = getAlertDAO();
        List alerts;

        if (pc.getSortattribute() == SortAttribute.NAME) {
            alerts = aDao.findByAppdefEntityInRangeSortByAlertDef(id, begin, 
                                                                  end);
        } else {
            alerts = aDao.findByAppdefEntityInRange(id, begin, end);
        }

        if (pc.getSortorder() == PageControl.SORT_DESC)
            Collections.reverse(alerts);

        return valuePager.seek(alerts, pc);
    }


    /**
     * Search alerts given a set of criteria
     *
     * @param timeRange the amount of milliseconds prior to current that the
     *                  alerts will be contained in.  e.g. the beginning of the  
     *                  time range will be (current - timeRante)
     *
     * @ejb:interface-method
     */
    public List findAlerts(AuthzSubjectValue subj, int count, int priority,
                           long timeRange, long endTime, List includes) 
        throws PermissionException 
    {
        AlertDAO aDao = getAlertDAO();
        List alerts;
            
        if (priority == EventConstants.PRIORITY_ALL) {
            alerts = aDao.findByCreateTime(endTime- timeRange, endTime, count);
        } else {
            alerts = aDao.findByCreateTimeAndPriority(endTime- timeRange,
                                                      endTime, priority, count);
        }
            
        List result = new ArrayList();
        Iterator it = alerts.iterator();
        for (int i = 0; it.hasNext(); i++) {
            Alert alert = (Alert) it.next();
            AlertDefinition alertdef = alert.getAlertDefinition();
            
            // Filter by appdef entity
            AppdefEntityID aeid = alertdef.getAppdefEntityId();
            if (includes != null && !includes.contains(aeid))
                continue;

            canManageAlerts(subj, aeid);
            // Finally add it
            result.add(alert);
        }
            
        return result;
    }

    /**
     * @ejb:interface-method
     */
    public List convertAlertsToEscalatables(Collection alerts) {
        List res = new ArrayList(alerts.size());

        for (Iterator i=alerts.iterator(); i.hasNext(); ) {
            Alert a = (Alert)i.next();
            
            res.add(new ClassicEscalatable(a, getShortReason(a),
                                           getLongReason(a)));
        }
        return res;
    }

    /**
     * Get the long reason for an alert
     * @ejb:interface-method
     */
    public String getShortReason(Alert alert) {
        // Get the short reason for the alert
        return alert.getAlertDefinition().getName();
    }
    
    /**
     * Get the long reason for an alert
     * @ejb:interface-method
     */
    public String getLongReason(Alert alert) {
        final String NOTAVAIL = "Not Available";
        final String indent = "    ";

        // Get the alert definition's conditions
        Collection clogs = alert.getConditionLog();
        
        AlertConditionLog[] logs = (AlertConditionLog[])
            clogs.toArray(new AlertConditionLog[clogs.size()]);

        StringBuffer text = new StringBuffer();
        for (int i = 0; i < logs.length; i++) {
            AlertCondition cond = logs[i].getCondition();

            if (i == 0) {
                text.append("\n").append(indent).append("If Condition: ");
            }
            else {
                text.append("\n").append(indent)
                    .append(cond.isRequired() ? "AND " : "OR ");
            }

//            TriggerFiredEvent event = (TriggerFiredEvent)
//            eventMap.get( cond.getTriggerId() );

            DerivedMeasurementDAO dmDao =
                DAOFactory.getDAOFactory().getDerivedMeasurementDAO();
            DerivedMeasurement dmv;
            
            switch (cond.getType()) {
            case EventConstants.TYPE_THRESHOLD:
            case EventConstants.TYPE_BASELINE:
                text.append(cond.getName()).append(" ")
                        .append(cond.getComparator()).append(" ");

                dmv = dmDao.findById(new Integer(cond.getMeasurementId()));

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
                    FormattedNumber th = UnitsConvert.convert(cond
                            .getThreshold(), dmv.getTemplate().getUnits());
                    text.append(th.toString());
                }

                // Make sure the event is present to be displayed
                /*
                 * FormattedNumber av = UnitsConvert.convert ( val,
                 * dmv.getTemplate().getUnits() );
                 */
                text.append(" (actual value = ").append(logs[i].getValue())
                        .append(")");
                break;
            case EventConstants.TYPE_CONTROL:
                text.append(cond.getName());
                break;
            case EventConstants.TYPE_CHANGE:
                dmv =
                    dmDao.findById(new Integer(cond.getMeasurementId()));
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

                double val = NumberUtil.stringAsNumber(logs[i].getValue())
                        .doubleValue();
                FormattedNumber av = UnitsConvert.convert(val, dmv
                        .getTemplate().getUnits());
                text.append(", new value = ").append(av.toString()).append(")");
                break;
            case EventConstants.TYPE_CUST_PROP:
                text.append(cond.getName()).append(" value changed");
                text.append("\n").append(indent).append(logs[i].getValue());
                break;
            case EventConstants.TYPE_LOG:
                text.append("Event/Log Level(")
                        .append(
                                ResourceLogEvent.getLevelString(Integer
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
            default:
                break;
            }
        }

        return text.toString();
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
