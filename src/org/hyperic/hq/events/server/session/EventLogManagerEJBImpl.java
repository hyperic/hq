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
import java.util.Collection;
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
            eval.setEntityType(aeId.getType());
            eval.setEntityId(aeId.getID());
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
     * Get the last log based on resource limited by time range
     * 
     * @ejb:interface-method
     */
    public List findLastLogs(int type, Integer[] ids, long begin)
    {
        return getEventLogDAO().findLastByEntity(type, ids, begin);
    }

    /** 
     * Get a list of log records based on resource, event type and time range
     * 
     * @ejb:interface-method
     */
    public List findLogs(AppdefEntityID ent, String[] eventTypes,
                         long begin, long end)
    {
        EventLogDAO eDAO = getEventLogDAO();
        return eDAO.findByEntity(ent, begin, end, eventTypes);
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
     * Get a list of log records based on resource, status and time range
     *
     * @ejb:interface-method
     */
    public List findLogs(AppdefEntityID ent, String status,
                         long begin, long end) 
    {
        return getEventLogDAO().findByEntityAndStatus(ent, begin, end, status);
    }

    public List findByCtime(long begin, long end, String[] eventTypes) {
        EventLogDAO dao = getEventLogDAO();
        return dao.findByCtime(begin, end, eventTypes);
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
     * @param entityId The entity.
     * @param begin The begin timestamp for the time range.
     * @param end The end timestamp for the time range.
     * @param intervals The number of intervals.
     * @return The boolean array with length equal to the number of intervals 
     *         specified.
     * @ejb:interface-method
     */
    public boolean[] logsExistPerInterval(AppdefEntityID entityId, 
                                          long begin, 
                                          long end, 
                                          int intervals) {
        
        boolean[] eventLogsInIntervals = new boolean[intervals];
        long interval = (end - begin) / intervals;       

        if (log.isDebugEnabled()) {
            log.debug("Checking if logs exist per interval: entity_type="+
                    entityId.getType()+", entity_id="+entityId.getID()+
                    ", begin="+begin+", end="+end+", intervals="+intervals+
                    ", interval="+interval);
        }
        
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT i FROM ").append(TABLE_EAM_NUMBERS)
           .append(" WHERE i < ").append(intervals)
           .append(" AND EXISTS (SELECT id FROM ").append(TABLE_EVENT_LOG)
           .append(" WHERE timestamp BETWEEN (")
           .append(begin).append(" + (").append(interval).append(" * i)) AND ((")
           .append(begin).append(" + (").append(interval).append(" * (i + 1))) - 1)")
           .append(" AND entity_id = ").append(entityId.getID())
           .append(" AND entity_type = ").append(entityId.getType())
           .append(' ')
           .append(Util.getHQDialect().getLimitString(1))
           .append(')');
        
        if (log.isDebugEnabled()) {
            log.debug(sql.toString());
        }
        
        Session sess = DAOFactory.getDAOFactory().getCurrentSession();       
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            conn = sess.connection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql.toString());

            while (rs.next()) {
                int index = rs.getInt(1);
                eventLogsInIntervals[index] = true;
            }
        } catch (SQLException e) {
            log.error("SQLException when fetching logs existence", e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            sess.disconnect();
        }

        return eventLogsInIntervals;
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
