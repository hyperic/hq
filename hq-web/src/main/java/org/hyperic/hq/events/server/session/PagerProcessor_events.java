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

import org.hyperic.hq.authz.server.session.Resource;
import org.hyperic.hq.common.SystemException;
import org.hyperic.hq.context.Bootstrap;
import org.hyperic.hq.escalation.shared.EscalationManager;
import org.hyperic.hq.events.shared.AlertValue;
import org.hyperic.util.pager.PagerEventHandler;
import org.hyperic.util.pager.PagerProcessorExt;

public class PagerProcessor_events implements PagerProcessorExt {
  

    public PagerProcessor_events () {}
    
    public PagerEventHandler getEventHandler() {
        return null;
    }
   
    public boolean skipNulls() {
        return true;
    }

    public Object processElement (Object o) {

        if (o == null) {
            return null;
        }

        try {
            if (o instanceof Alert) {
                Alert alert = (Alert) o;
                AlertValue aval = alert.getAlertValue();
                aval.setAcknowledgeable(
                    Bootstrap.getBean(EscalationManager.class).isAlertAcknowledgeable(
                        alert.getId(), alert.getAlertDefinition()));
                return aval; 
            } else if (o instanceof AlertDefinition) {
                AlertDefinition def = (AlertDefinition) o;
                Resource r = def.getResource();
                if (r == null || r.isInAsyncDeleteState()) {
                    return null;
                } else {
                    return def.getAlertDefinitionValue();
               }
            }
        } catch (Exception e) {
            throw new SystemException("Error converting " + o +
                                      " to value object: " + e, e);
        }

        return o;
    }
    
    public Object processElement(Object o1, Object o2) {
        return processElement(o1);
    }
}
