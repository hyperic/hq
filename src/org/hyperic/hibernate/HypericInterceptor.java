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

package org.hyperic.hibernate;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.hyperic.hq.appdef.AppdefBean;
import org.hyperic.hq.authz.server.session.ResourceGroup;
import org.hyperic.hq.escalation.server.session.Escalation;
import org.hyperic.hq.escalation.server.session.EscalationState;
import org.hyperic.hq.events.server.session.AlertDefinition;
import org.hyperic.hq.measurement.server.session.Measurement;
import org.hyperic.hq.measurement.server.session.MeasurementTemplate;
import org.hyperic.hq.product.Plugin;

/**
 * multi-purpose interceptor for injecting runtime logic,
 *
 * One use case is to set creation and modified time on
 * on save, merge or collection cascades
 *
 * TODO:  Consolidate into regular UserTyped AuditInfo stylee 
 */
public class HypericInterceptor 
    extends EmptyInterceptor
{
    private final Log _log = LogFactory.getLog(HypericInterceptor.class);
    
    private boolean entHasTimestamp(Object o) {
        return o instanceof Plugin ||
               o instanceof AppdefBean ||
               o instanceof AlertDefinition ||
               o instanceof Escalation ||
               o instanceof EscalationState ||
               o instanceof MeasurementTemplate ||
               o instanceof Measurement ||
               o instanceof ResourceGroup;
    }
    
    public boolean onFlushDirty(Object entity, Serializable id, 
                                Object[] currentState, Object[] previousState, 
                                String[] propertyNames, Type[] types)
    {
        if (entHasTimestamp(entity))
            return updateTimestamp(currentState, previousState, propertyNames);
        return false;
    }

    public boolean onSave(Object entity, Serializable id, Object[] state, 
                          String[] propertyNames, Type[] types)
    {
        if (entHasTimestamp(entity))
            return updateTimestamp(state, null, propertyNames);
        return false;
    }

    private boolean updateTimestamp(Object[] curState, Object[] prevState,
                                    String[] propertyNames) {
        boolean modified = false;
        long ts = System.currentTimeMillis();
        int modifiedIdx = -1;
        int createdIdx = -1;
        for (int i = 0; i < propertyNames.length; i++) {
            if (prevState != null) {
                if (curState[i] == null && prevState[i] != null) {
                    modified = true;
                } else if (curState[i] != null && prevState[i] == null) {
                    modified = true;
                } else if (curState[i] != null && prevState[i] != null &&
                    !curState[i].equals(prevState[i])) {
                    modified = true;
                }
            }
            if ("creationTime".equals(propertyNames[i]) ||
                "ctime".equals(propertyNames[i])) 
            {
                Long ctime = (Long)curState[i];
                if (ctime == null || ctime.longValue() == 0) {
                    createdIdx = i;
                    modified =  true;
                }
            } else if ("modifiedTime".equals(propertyNames[i]) ||
                       "mtime".equals(propertyNames[i]))
            {
                modifiedIdx = i;
                modified = true;
            }
        }
        if (createdIdx >= 0) {
            curState[createdIdx] = new Long(ts);
        }
        if (modifiedIdx >= 0 && modified) {
            curState[modifiedIdx] = new Long(ts);
        }
        return modified;
    }
}
