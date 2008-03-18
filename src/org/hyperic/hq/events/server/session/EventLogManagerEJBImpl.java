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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.hibernate.Session;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hibernate.PageInfo;
import org.hyperic.hibernate.Util;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.authz.server.session.ResourceManagerEJBImpl;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.EventLogStatus;
import org.hyperic.hq.events.ResourceEventInterface;
import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.events.shared.EventLogManagerLocal;
import org.hyperic.hq.events.shared.EventLogManagerUtil;
import org.hyperic.hq.measurement.MeasurementConstants;
import org.hyperic.hq.product.TrackEvent;
import org.hyperic.util.jdbc.DBUtil;

/**
 * <p> Stores Events to and deletes Events from storage</p>
 *
 * @ejb:bean name="EventLogManager"
 *      jndi-name="ejb/events/EventLogManager"
 *      local-jndi-name="LocalEventLogManager"
 *      view-type="local"
 *      type="Stateless"
 *
 * @ejb:transaction type="REQUIRED"
 */
public class EventLogManagerEJBImpl extends SessionBase implements SessionBean {
    private final String logCtx =
        EventLogManagerEJBImpl.class.getName();
    
    private final String TABLE_EVENT_LOG = "EAM_EVENT_LOG";
    private final String TABLE_EAM_NUMBERS = "EAM_NUMBERS";
    
    private static final int MSGMAX = TrackEvent.MESSAGE_MAXLEN;
    private static final int SRCMAX = TrackEvent.SOURCE_MAXLEN;
    
    private EventLogDAO getEventLogDAO() {
        return new EventLogDAO(DAOFactory.getDAOFactory());
    }
    
    /** 
     * Create a new vanilla log item.
     * 
     * @param event The event to log.
     * @param subject The log item subject.
     * @param status The log item status.
     * @param save <code>true</code> to persist the log item; 
     *             <code>false</code> to create a transient log item only.
     * 
     * @ejb:interface-method
     */
    public EventLog createLog(AbstractEvent event, 
                              String subject,
                              String status, 
                              boolean save) {
        EventLog eval = new EventLog();

        // Set the time to the event time
        eval.setTimestamp(event.getTimestamp());

        // Must set the detail and the type
        eval.setType(event.getClass().getName());

        String detail = event.toString();
        if (detail.length() > MSGMAX) {
            detail = detail.substring(0, MSGMAX - 1);
        }

        eval.setDetail(detail);

        if (status != null)
            eval.setStatus(status);

        if (subject != null) {
            if (subject.length() > SRCMAX) {
                subject = subject.substring(0, SRCMAX - 1);
            }
            eval.setSubject(subject);
        }

        if (event instanceof ResourceEventInterface) {
            AppdefEntityID aeId =
                ((ResourceEventInterface) event).getResource();
            Resource r = ResourceManagerEJBImpl.getOne().findResource(aeId);
            eval.setResource(r);
        }

        if (save) {
            return getEventLogDAO().create(eval);            
        } else {
            return eval;
        }
    }
    
    /**
     * Insert the event logs in batch.
     * 
     * @param eventLogs The event logs.
     * 
     * @ejb:interface-method
     */
    public void insertEventLogs(EventLog[] eventLogs) {
        getEventLogDAO().insertLogs(eventLogs);
    }
    
    /** 
     * Find the last event logs of all the resources of a given prototype.
     * (i.e. 'Linux' or 'FileServer File')
     * 
     * @ejb:interface-method
     */
    public List findLastLogs(Resource proto) {
        return getEventLogDAO().findLastByType(proto);
    }
    
    /** 
     * Get a list of {@link ResourceEventLog}s in a given interval, with
     * the maximum specified status.
     * 
     * If specified, typeClass dictates the full classname of the rows
     * to check (i.e. org.hyperic.hq.....ResourceLogEvent)
     * 
     * If specified, inGroups must be a collection of {@link ResourceGroup}s
     * which the resulting logs will be associated with.
     * 
     * @ejb:interface-method
     */
    public List findLogs(AuthzSubject subject, long begin, long end, 
                         PageInfo pInfo,
                         EventLogStatus maxStatus, String typeClass,
                         Collection inGroups)
    {
        return getEventLogDAO().findLogs(subject, begin, end, pInfo, maxStatus, 
                                         typeClass, inGroups);
    }

    /** 
     * Get a list of log records based on resource, event type and time range.
     * All resources which are descendents of the passed resource will also
     * have their event logs included
     * 
     * @ejb:interface-method
     */
    public List findLogs(AppdefEntityID ent, AuthzSubject user, 
                         String[] eventTypes, long begin, long end)
    {
        EventLogDAO eDAO = getEventLogDAO();
        Resource r = ResourceManagerEJBImpl.getOne().findResource(ent);
        Collection eTypes;
        
        if (eventTypes == null)
            eTypes = Collections.EMPTY_LIST;
        else
            eTypes = Arrays.asList(eventTypes);
        return eDAO.findByEntity(user, r, begin, end, eTypes);
    }

    /** 
     * Get a list of log records based on resource, status and time range.
     * All resources which are descendants of the passed resource will also
     * have their event logs included
     *
     * @ejb:interface-method
     */
    public List findLogs(AppdefEntityID ent, AuthzSubject user, String status,
                         long begin, long end) 
    {
        Resource r = ResourceManagerEJBImpl.getOne().findResource(ent);
        return getEventLogDAO().findByEntityAndStatus(r, user, begin, end, 
                                                      status);
    }

    /**
     * Retrieve the total number of event logs.
     * 
     * @return The total number of event logs.
     * @ejb:interface-method
     */
    public int getTotalNumberLogs() {
        return getEventLogDAO().getTotalNumberLogs();
    }
    
    /**
     * Get an array of booleans, each element indicating whether or not there 
     * are log records for that respective interval, for a particular entity 
     * over a given time range.
     * 
     * This method also takes descendents of the passed-resource into
     * consideration.
     * 
     * @param entityId The entity.
     * @param begin The begin timestamp for the time range.
     * @param end The end timestamp for the time range.
     * @param intervals The number of intervals.
     * @return The boolean array with length equal to the number of intervals 
     *         specified.
     * @ejb:interface-method
     */
    public boolean[] logsExistPerInterval(AppdefEntityID entityId,
                                          AuthzSubject subject,
                                          long begin, long end,  
                                          int intervals) 
    {
        Resource r = ResourceManagerEJBImpl.getOne().findResource(entityId);
        return getEventLogDAO().logsExistPerInterval(r, subject, begin, end, 
                                                     intervals);
    }

    /** 
     * Purge old event logs.
     * 
     * @param from Delete all records starting from (and including) this time.
     * If set to -1, then this method will delete all records from the
     * earliest record forward.
     * @param to Delete all records up to (and including) this time. 
     * If set to -1, then this method will delete all records up to and 
     * including the most recent record.
     * @return The number of records removed.
     * @ejb:interface-method
     * @ejb:transaction type="NOTSUPPORTED"
     */
    public int deleteLogs(long from, long to) { 
        if (log.isDebugEnabled()) {
            log.debug("deleteLogs(" + from + ", " + to + ")");
        }
        
        if (from == -1) {
            from = getEventLogDAO().getMinimumTimeStamp();
            
            if (from == -1) {
                return 0;
            }
        }
        
        if (to == -1) {
            to = System.currentTimeMillis();
        }
        
        if (log.isDebugEnabled()) {
            log.debug("updated deleteLogs(" + from + ", " + to + ")");
        }
        
        if (from > to) {
            log.debug("deleteLogs range has (from > to). There are no rows to delete.");
            return 0;
        }
        
        // Now that we have valid from/to values, figure out what the
        // interval is (don't loop more than 60 times)
        long interval = Math.max(MeasurementConstants.DAY,
                                 (to - from) / 60);
        
        return getEventLogDAO().deleteLogs(from, to, interval);
    }

    /**
     * @ejb:create-method
     */
    public void ejbCreate() {}
    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() {}
    public void setSessionContext(SessionContext ctx) {}

    public static EventLogManagerLocal getOne() {
        try {
            return EventLogManagerUtil.getLocalHome().create();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        }
    }
}
