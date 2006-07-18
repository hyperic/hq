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

package org.hyperic.hq.agent.server;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JdbcProviderIterator implements Iterator {
    private ResultSet           m_rset;
    private AgentJdbcProvider   m_provider;
    private boolean             m_lookedahead;
    private int                 m_removeReady;
        
    private Log                 m_logger = 
        LogFactory.getLog(AgentJdbcProvider.class);
    
    JdbcProviderIterator(ResultSet rset, boolean prefetch, 
                         AgentJdbcProvider provider) 
    {
        this.m_rset        = rset;
        this.m_lookedahead = prefetch;
        this.m_provider    = provider;
    }
    
    public boolean hasNext() {
        // If hasNext() is being called multiple times without calling next
        // than we return true because m_lookedahead will be true on the
        // previous call
        if(m_lookedahead == true)
            return true;
            
        try {
            boolean result = this.m_rset.next();
            m_lookedahead = result;
            return result;
        }
        catch(SQLException e) {
            m_lookedahead = false;
            return false;
        }
    }

    public Object next() {
        try {
            // If hasNext() was not called, we need to call next() on the 
            // result set now
            if(m_lookedahead == false) {
                this.m_rset.next();
                this.m_logger.debug("next member!");
            } else {
                // hasNext and next are on the same record
                m_lookedahead = false;   
            }

            m_removeReady = 1;
            
            return this.m_rset.getString(1);
        }
        catch(SQLException e) {
            this.m_logger.error("JdbcProviderIterator.next() failed.");
            throw new NoSuchElementException(e.getMessage());
        }
    }

    public void remove() {
        // Check if next was called prior to calling remove
        if(m_removeReady == 0)
            throw new IllegalStateException(
                "remove() called without first calling next()" );

        // Decrement the next check flag, hopefully back to 0
        m_removeReady --;
        
        // If removeReady is below 0, remove is being called multiple times
        // without calling next
        if(m_removeReady < 0) {
            m_removeReady ++; // We'll put the ready count back
            throw new IllegalStateException(
                "remove() called more than once after next()" );
        }
                      
        try {
            this.m_provider.deleteMetric( this.m_rset.getLong(2) );
        }
        catch(SQLException e) {
            this.m_logger.error("Remove value failed: " + e);
        }
    }
}
