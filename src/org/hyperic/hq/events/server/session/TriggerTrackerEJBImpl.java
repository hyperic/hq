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

import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.ejb.EJBException;
import javax.ejb.SessionBean;
import javax.ejb.SessionContext;
import javax.naming.NamingException;

import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.events.shared.TriggerTrackerLocal;
import org.hyperic.hq.events.shared.TriggerTrackerUtil;
import org.hyperic.util.jdbc.DBUtil;

/**
 * <p> Stores log of trigger execution, so that it can be used to determine
 * whether or not to suppress additional triggers
 * 
 * </p>
 * @ejb:bean name="TriggerTracker"
 *      jndi-name="ejb/events/TriggerTracker"
 *      local-jndi-name="LocalTriggerTracker"
 *      view-type="local"
 *      type="Stateless"
 *
 */
public class TriggerTrackerEJBImpl extends SessionBase implements SessionBean {
    private final String logCtx =
        "org.hyperic.hq.events.server.session.TriggerTrackerEJBImpl";

    private final String TAB_FIRED_TRIGGER = "EAM_FIRED_TRIGGER";

    private SessionContext ctx = null;
    
    ///////////////////////////////////////
    // operations


    /** Returns whether or not to fire a trigger, based on the frequency.
     * If trigger should fire, then register the time that it fired.
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     * @param tid the Trigger ID
     */
    public boolean fire(Integer tid, long frequency) {
        long current = System.currentTimeMillis();
        
        // Look up any triggers that have fired in the past
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = DBUtil.getConnByContext(getInitialContext(), DATASOURCE);

            StringBuffer strBuf = new StringBuffer()
                .append("SELECT timestamp FROM ")
                .append(TAB_FIRED_TRIGGER)
                .append(" WHERE trigger_id = ?");

            long last = 0;
            try {
                stmt = conn.prepareStatement(strBuf.toString());

                stmt.setInt(1, tid.intValue());
                rs = stmt.executeQuery();
    
                if (rs.next()) {
                    last = rs.getLong(1);            
                }
            } finally {
                DBUtil.closeJDBCObjects(logCtx, null, stmt, rs);
            }

            // See if we update or insert
            strBuf = new StringBuffer();
            if (last > 0) {
                // Update            
                strBuf.append("UPDATE ")
                      .append(TAB_FIRED_TRIGGER)
                      .append(" SET timestamp = ? WHERE trigger_id = ?");
            }
            else {
                // Insert
                if (DBUtil.isOracle(conn)) {
                    strBuf.append("INSERT INTO ")
                        .append(TAB_FIRED_TRIGGER)
                        .append(" (id, timestamp, trigger_id) VALUES")
                        .append(" (EAM_FIRED_TRIGGER_ID_SEQ.nextval,?,?)");
                } else if (DBUtil.isMySQL(conn)) {
                    strBuf.append("INSERT INTO ")
                        .append(TAB_FIRED_TRIGGER)
                        .append(" (id, timestamp, trigger_id) VALUES")
                        .append(" (nextseqval('EAM_FIRED_TRIGGER_ID_SEQ'),?,?)");
                } else {
                    strBuf.append("INSERT INTO ")
                        .append(TAB_FIRED_TRIGGER)
                        .append(" (timestamp, trigger_id) VALUES (?,?)");
                }
            }

            // Now add the Trigger to Event relationship
            stmt = conn.prepareStatement(strBuf.toString());
            
            int i = 1;
            stmt.setLong(i++, current);
            stmt.setInt(i++, tid.intValue());
            stmt.execute();

            // Let's check the time to see if it's at least within the last hour
            long avoid = Math.max(60 * 60 * 1000, frequency);
            if (last > current - avoid)
                return false;

        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (SQLException e) {
            // If we can't look up the history, then we just have to allow
            // trigger to fire
        } finally {
            DBUtil.closeStatement(logCtx, stmt);
            DBUtil.closeConnection(logCtx, conn);
        }

        return true;
    } // end fire        
    
    /**
     * Removes any record of this trigger having fired
     * @param tid the Trigger ID
     *
     * @ejb:interface-method
     * @ejb:transaction type="REQUIRED"
     */
    public void remove(Integer tid) {
        // Look up any triggers that have fired in the past
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = DBUtil.getConnByContext(getInitialContext(), DATASOURCE);
    
            StringBuffer strBuf =
                new StringBuffer("DELETE FROM ")
                    .append(TAB_FIRED_TRIGGER)
                    .append(" WHERE trigger_id = ?");
    
            stmt = conn.prepareStatement(strBuf.toString());
            
            stmt.setInt(1, tid.intValue());
            stmt.execute();
        } catch (NamingException e) {
            throw new SystemException(e);
        } catch (SQLException e) {
            // Assume that we had nothing to delete, then
        } finally {
            DBUtil.closeStatement(logCtx, stmt);
            DBUtil.closeConnection(logCtx, conn);
        }
    }

    public static TriggerTrackerLocal getOne() {
        try {
            return TriggerTrackerUtil.getLocalHome().create(); 
        } catch(Exception e) {
            throw new SystemException(e);
        }
    }    
    
    ///////////////////////////////////////
    // EJB operations

    /**
     * @see javax.ejb.SessionBean#ejbCreate()
     * @ejb:create-method
     */
    public void ejbCreate() {}

    /**
     * @see javax.ejb.SessionBean#ejbPostCreate()
     */
    public void ejbPostCreate() {}

    /**
     * @see javax.ejb.SessionBean#ejbActivate()
     */
    public void ejbActivate() {}

    /**
     * @see javax.ejb.SessionBean#ejbPassivate()
     */
    public void ejbPassivate() {}

    /**
     * @see javax.ejb.SessionBean#ejbRemove()
     */
    public void ejbRemove() {
        this.ctx = null;
    }

    /**
     * @see javax.ejb.SessionBean#setSessionContext(SessionContext)
     */
    public void setSessionContext(SessionContext ctx)
        throws EJBException, RemoteException {
        this.ctx = ctx;
    }

} 



