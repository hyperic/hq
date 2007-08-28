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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.AbstractEvent;
import org.hyperic.util.jdbc.DBUtil;

/**
 * Stores Events to and deletes Events from storage
 * @ejb:bean name="EventTracker"
 *      jndi-name="ejb/events/EventTracker"
 *      local-jndi-name="LocalEventTracker"
 *      view-type="local"
 *      type="Stateless"
 *      
 * @ejb:transaction type="NOTSUPPORTED"
 */
public class EventTrackerEJBImpl extends SessionBase implements SessionBean {
    private final String logCtx =
        "org.hyperic.hq.events.server.session.EventTrackerEJBImpl";
    private final Log log = LogFactory.getLog(logCtx);

    private final String SEQ_EVENT = "EAM_EVENT_ID_SEQ";
    private final String TAB_EVENT = "EAM_EVENT";
    private final String TAB_TRIGGER_EVENT = "EAM_TRIGGER_EVENT";
    private final String SQL_CLEANUP =
        "DELETE FROM " + TAB_EVENT +
        " WHERE ctime < ? AND 0 = (SELECT COUNT(*) FROM " +
             TAB_TRIGGER_EVENT + " WHERE event_id = id)";
    
    private SessionContext ctx = null;

    ///////////////////////////////////////
    // operations

    /** Add a reference from a Trigger to an Event
     * @ejb:interface-method
     * @param tid the Trigger ID
     * @param event the referenced Event
     */
    public void addReference(Integer tid, AbstractEvent event, long expiration)
        throws SQLException, IOException {
        if (log.isDebugEnabled())
            log.debug("Add reference for trigger ID: " + tid);
            
        Connection conn = null;
        PreparedStatement stmt = null;
        StringBuffer strBuf;
        try {
            conn = DBUtil.getConnByContext(getInitialContext(), DATASOURCE);

            // XXX: Need to make the two inserts atomic
            if (event.getId() == null) {
                // Get the next Event ID
                event.setId(getNextId(SEQ_EVENT)); 

                ByteArrayOutputStream ostream = new ByteArrayOutputStream();
                ObjectOutputStream p = new ObjectOutputStream(ostream);
                p.writeObject(event);
                p.flush();
                ostream.close();

                strBuf = new StringBuffer()
                    .append("INSERT INTO ")
                    .append(TAB_EVENT)
                    .append(" (ID, EVENT_OBJECT, CTIME) VALUES (?,?,?)");

                try {
                    stmt = conn.prepareStatement(strBuf.toString());

                    // Insert into Event table first
                    int i = 1;
                    stmt.setInt(i++, event.getId().intValue());
                    stmt.setBytes(i++, ostream.toByteArray());
                    stmt.setLong(i++, event.getTimestamp());
                    stmt.execute();
                } finally {
                    DBUtil.closeStatement(logCtx, stmt);
                }
            }

            // Now add the Trigger to Event relationship
            strBuf = new StringBuffer()
                .append("INSERT INTO ")
                .append(TAB_TRIGGER_EVENT)
                .append(" (trigger_id, event_id, expiration) ")
                .append("VALUES (?,?,?)");

            try {
                stmt = conn.prepareStatement(strBuf.toString());

                int i = 0;
                stmt.setInt(++i, tid.intValue());
                stmt.setLong(++i, event.getId().longValue());
                long expire = (expiration == 0 ? Long.MAX_VALUE :
                                expiration + System.currentTimeMillis());
                stmt.setLong(++i, expire);
                stmt.execute();
            } finally {
                DBUtil.closeStatement(logCtx, stmt);
            }
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (CreateException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
        }
    } // end addReference        


    ///////////////////////////////////////
    // operations

    /** Add a reference from a Trigger to an Event
     * @ejb:interface-method
     * @param tid the Trigger ID
     * @param event the referenced Event
     */
    public void updateReference(Integer tid, Long eid, AbstractEvent event)
        throws SQLException, IOException {
        Connection conn = null;
        PreparedStatement stmt = null;
        StringBuffer strBuf;
        try {
            conn = DBUtil.getConnByContext(getInitialContext(), DATASOURCE);

            // Now update the new event
            event.setId(eid);

            ByteArrayOutputStream ostream = new ByteArrayOutputStream();
            ObjectOutputStream p = new ObjectOutputStream(ostream);
            p.writeObject(event);
            p.flush();
            ostream.close();

            strBuf = new StringBuffer()
                .append("UPDATE ")
                .append(TAB_EVENT)
                .append(" SET EVENT_OBJECT = ?, CTIME = ?")
                .append(" WHERE ID = ?");

            try {
                stmt = conn.prepareStatement(strBuf.toString());

                // Insert into Event table first
                int i = 1;
                stmt.setBytes(i++, ostream.toByteArray());
                stmt.setLong(i++, event.getTimestamp());
                stmt.setLong(i++, eid.longValue());
                stmt.execute();
            } finally {
                DBUtil.closeStatement(logCtx, stmt);
            }
            
            // Clean up the old events
            /*
            try {
                stmt = conn.prepareStatement(SQL_CLEANUP);
                stmt.setLong(1, System.currentTimeMillis() - 10000);
                stmt.execute();
            } finally {
                DBUtil.closeStatement(logCtx, stmt);
            }
            */
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
        }
    } // end addReference        


    /** Remove all references of a Trigger
     * @ejb:interface-method
     * @param tid the Trigger ID
     */
    public void deleteReference(Integer tid) throws SQLException {
        if (log.isDebugEnabled())
            log.debug("Delete references for trigger ID: " + tid);

        Connection conn = null;
        PreparedStatement stmt = null;
        StringBuffer strBuf;
        try {
            long current = System.currentTimeMillis();
            conn = DBUtil.getConnByContext(getInitialContext(), DATASOURCE);

            // First delete the trigger references
            strBuf = new StringBuffer()
                .append("DELETE FROM ") 
                .append(TAB_TRIGGER_EVENT)
                .append(" WHERE trigger_id = ? OR expiration < ?");

            try {
                stmt = conn.prepareStatement(strBuf.toString());
                    
                int i = 1;
                stmt.setInt(i++, tid.intValue());
                stmt.setLong(i++, current);
                stmt.execute();
            } finally {
                // Close the statement
                DBUtil.closeStatement(logCtx, stmt);
            }

            // Next, get the events to be deleted.  Only delete events from
            // 10 seconds ago to avoid deleting event created in addRef(), but
            // have not been referenced by the triggers yet
            try {
                stmt = conn.prepareStatement(SQL_CLEANUP);
                stmt.setLong(1, current - 10000);
                stmt.execute();
            } finally {
                // Close the statement
                DBUtil.closeStatement(logCtx, stmt);
            }
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeConnection(logCtx, conn);
        }
    } // end dispose        


    /** Get the list of Events that are referenced by a given Trigger in order
     * of reference creation
     * @ejb:interface-method
     * @param tid the Trigger ID
     * @return the list of ObjectInputStream's (Events) referenced by Trigger
     */
    public LinkedList getReferencedEventStreams(Integer tid)
        throws SQLException, IOException {
        if (log.isDebugEnabled())
            log.debug("Get references for trigger ID: " + tid);

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        StringBuffer strBuf;
        LinkedList events = new LinkedList();
        try {
            conn = DBUtil.getConnByContext(getInitialContext(), DATASOURCE);

            // Order by event_id, since it's sequential
            strBuf = new StringBuffer()
                .append("SELECT event_object FROM ")
                .append(TAB_EVENT)
                .append(" e, ")
                .append(TAB_TRIGGER_EVENT)
                .append(" t WHERE e.id=t.event_id AND t.trigger_id = ? AND ")
                .append("t.expiration > ? ORDER BY ctime");

            stmt = conn.prepareStatement(strBuf.toString());

            int i = 0;
            stmt.setInt(++i, tid.intValue());
            stmt.setLong(++i, System.currentTimeMillis());
            rs = stmt.executeQuery();

            while (rs.next()) {
                // Materialize the blob using the java.sql.Blob which
                // should help the driver understand it needs to return
                // the locater - not the data.
                byte [] data = DBUtil.getBlobColumn(rs, 1);

                ByteArrayInputStream istream = new ByteArrayInputStream(data);
                events.add(new ObjectInputStream(istream));
            }
        } catch (NamingException e) {
            throw new SystemException(e);
        } finally {
            DBUtil.closeJDBCObjects(logCtx, conn, stmt, rs);
        }

        return events;
    } // end storeEvent        

    /** @ejb:create-method */
    public void ejbCreate() {}
    public void ejbPostCreate() {}
    public void ejbActivate() {}
    public void ejbPassivate() {}
    public void ejbRemove() { this.ctx = null; }
    public void setSessionContext(SessionContext ctx)
        throws EJBException, RemoteException {
        this.ctx = ctx;
    }
}



