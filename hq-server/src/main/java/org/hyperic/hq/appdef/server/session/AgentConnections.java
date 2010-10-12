/**
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2009-2010], VMware, Inc.
 *  This file is part of HQ.
 *
 *  HQ is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.hq.appdef.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Maintains a list of active agent connections
 */
public class AgentConnections {
    private static final AgentConnections INSTANCE = new AgentConnections();

    private final Log _log = LogFactory.getLog(AgentConnections.class);

    // Map of (String) method -> (Set) of AgentConnection
    private Map  _activeConns = new HashMap();
    
    // Aggregate conns over the lifetime of the process
    private long _totalConns = 0; 
    
    public static class AgentConnection {
        private Object  _storeToken;
        private String  _method;          // Method it is running
        private String  _connIp;          // Connection IP
        private long    _connectTime;     // Time it connected
        private Integer _id;              // Optional agent ID
        
        public String getMethod() {
            return _method;
        }
        
        public String getConnectedIp() {
            return _connIp;
        }
        
        public long getConnectTime() {
            return _connectTime;
        }
        
        public Integer getAgentId() {
            return _id;
        }
        
        public boolean equals(Object obj) {
            if (obj == null || obj instanceof AgentConnection == false)
                return false;
            
            return ((AgentConnection)obj)._storeToken == _storeToken;
        }
        
        public int hashCode() {
            return _storeToken.hashCode();
        }
    }

    public int getNumConnected() {
        int res = 0;
        synchronized (_activeConns) {
            for (Iterator i=_activeConns.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry ent = (Map.Entry)i.next();
                Set agents = (Set)ent.getValue();
                res += agents.size();
            }
        }
        return res;
    }
    
    Collection getConnected() {
        List res = new ArrayList();
        
        synchronized (_activeConns) {
            for (Iterator i=_activeConns.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry ent = (Map.Entry)i.next();
                Set agents = (Set)ent.getValue();
                
                res.addAll(agents);
            }
        }
        return res;
    }
    
    AgentConnection agentConnected(String method, String connIp, Integer id) {
        AgentConnection newConn = new AgentConnection();
        newConn._storeToken  = new Object();
        newConn._method      = method;
        newConn._connIp      = connIp;
        newConn._connectTime = System.currentTimeMillis();
        newConn._id          = id;

        synchronized (_activeConns) {
            Set conns = (Set)_activeConns.get(method);
            
            if (conns == null) {
                conns = new HashSet(1);
                _activeConns.put(method, conns);
            }
            
            conns.add(newConn);
            _totalConns++;
        }
        return newConn;
    }
    
    void disconnectAgent(AgentConnection c) {
        synchronized (_activeConns) {
            Set conns = (Set)_activeConns.get(c.getMethod());
            
            if (conns == null) {
                _log.warn("AgentConnection [" + c.getMethod() + "] " + 
                          "disconnected, but method wasn't found");
                return;
            }
            conns.remove(c);
        }
    }
    
    public long getTotalConnections() {
        synchronized (_activeConns) {
            return _totalConns;
        }
    }
    
    public static AgentConnections getInstance() {
        return INSTANCE;
    }
}
