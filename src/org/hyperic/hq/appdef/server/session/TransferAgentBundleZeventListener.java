/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

package org.hyperic.hq.appdef.server.session;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AgentManagerLocal;
import org.hyperic.hq.authz.server.session.AuthzSubject;
import org.hyperic.hq.authz.server.session.AuthzSubjectManagerEJBImpl;
import org.hyperic.hq.zevents.ZeventListener;

/**
 * The Zevent Listener that transfers agent bundles.
 */
public class TransferAgentBundleZeventListener implements ZeventListener {
    
    private final Log _log = LogFactory.getLog(TransferAgentBundleZeventListener.class);

    /**
     * @see org.hyperic.hq.zevents.ZeventListener#processEvents(java.util.List)
     */
    public void processEvents(List events) {
        AgentManagerLocal agentMan = AgentManagerEJBImpl.getOne();
        AuthzSubject overlord = AuthzSubjectManagerEJBImpl.getOne().getOverlordPojo();
        
        for (Iterator iter = events.iterator(); iter.hasNext();) {
            TransferAgentBundleZevent zevent = (TransferAgentBundleZevent) iter.next();
            
            try {
                agentMan.transferAgentBundle(overlord, 
                                             zevent.getAgent(), 
                                             zevent.getAgentBundleFile());
            } catch (Exception e) {
                _log.warn("Failed to transfer agent bundle "+
                          zevent.getAgentBundleFile()+ " to agent "+
                          zevent.getAgent().getID(), e);
            }
        }

    }
    
    public String toString() {
        return "AgentBundleTransfer";
    }

}
