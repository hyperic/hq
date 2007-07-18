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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import javax.ejb.CreateException;
import javax.ejb.RemoveException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.hibernate.Session;
import org.hyperic.dao.DAOFactory;
import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.hq.events.ResourceEventInterface;
import org.hyperic.hq.events.server.session.EventLog;
import org.hyperic.hq.events.shared.EventLogManagerLocal;
import org.hyperic.hq.events.shared.EventLogManagerUtil;
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

    private static final int MSGMAX = 500;
    private static final int SRCMAX = 100;
    
    private EventLogDAO getEventLogDAO() {
        return DAOFactory.getDAOFactory().getEventLogDAO();
    }
    
    /** 
     * Create a new vanilla log item
     * 
     * @ejb:interface-method
     */
    public EventLog createLog(AbstractEvent event, String subject,
                              String status) {
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

        return getEventLogDAO().create(eval);
    }

    /** 
     * Get a list of log records based on resource, event type and time range
     * 
     * @ejb:interface-method
     */
    public List findLogs(int entityType, int entityId, String[] eventTypes,
                         long begin, long end)
    {
        EventLogDAO eDAO = getEventLogDAO();
        AppdefEntityID entId = new AppdefEntityID(entityType, entityId);
        
        return eDAO.findByEntity(entId, begin, end, eventTypes);
    }

    /** 
     * Get a list of log records based on resource, status and time range
     *
     * @ejb:interface-method
     */
    public List findLogs(int entityType, int entityId, String status,
                         long begin, long end) 
    {
        EventLogDAO eDAO = getEventLogDAO();
        AppdefEntityID ent = new AppdefEntityID(entityType, entityId);
        
        return eDAO.findByEntityAndStatus(ent, begin, end, status);
    }

    /**
     * Get an array of log record counts based on entity ID and time range
     * 
     * @ejb:interface-method
     */
    public int[] getLogsCount(AppdefEntityID entityId, long begin, long end,
                              int intervals)
    {
        final String sql =
            "SELECT i, COUNT(e.id) FROM " + TABLE_EVENT_LOG + " e, EAM_NUMBERS " +
            "WHERE i < ? AND entity_type = ? AND entity_id = ? AND " +
                  "timestamp BETWEEN (? + (? * i)) AND (? + (? * i)) " +
            "GROUP BY i ORDER BY i";
    
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        Session sess = DAOFactory.getDAOFactory().getCurrentSession();
        
        int[] ret = new int[intervals];
        Arrays.fill(ret, 0);
        
        try {
            conn = sess.connection();
            stmt = conn.prepareStatement(sql);
            
            long interval = (end - begin) / intervals;
            
            int i = 1;
            stmt.setInt(i++, intervals);
            stmt.setInt(i++, entityId.getType());
            stmt.setInt(i++, entityId.getID());
            stmt.setLong(i++, begin);
            stmt.setLong(i++, interval);
            stmt.setLong(i++, begin + interval);
            stmt.setLong(i++, interval);
    
            rs = stmt.executeQuery();
            
            while(rs.next()) {
                int index = rs.getInt(1);
                ret[index] = rs.getInt(2);
            }
        } catch (SQLException e) {
            log.error("SQLException when fetching logs existence", e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            sess.disconnect();
        }
        
        return ret;
    }

    /** 
     * Purge old logs
     *
     * @ejb:interface-method
     */
    public void deleteLogs(long begin, long end) throws RemoveException {
        getEventLogDAO().deleteLogs(begin, end);
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
