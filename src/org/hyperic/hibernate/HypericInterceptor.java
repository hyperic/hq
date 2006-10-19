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

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.hyperic.hq.appdef.AppdefBean;

import java.io.Serializable;

/**
 * multi-purpose interceptor for injecting runtime logic,
 *
 * One use case is to set creation and modified time on
 * on save, merge or collection cascades
 */
public class HypericInterceptor extends EmptyInterceptor
{
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types)
    {
        if (entity instanceof AppdefBean) {
            return processAppdefBean((AppdefBean)entity, id, currentState, propertyNames, types);
        }
        return false;
    }

    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)
    {
        if (entity instanceof AppdefBean) {
            return processAppdefBean((AppdefBean)entity, id, state, propertyNames, types);
        }
        return false;
    }

    private boolean processAppdefBean(AppdefBean appdef, Serializable id, Object[] state, String[] propertyNames, Type[] types)
    {
        boolean modified = updateTimestamp(state, propertyNames);
        return modified;
    }

    private boolean updateTimestamp(Object[] state, String[] propertyNames)
    {
        boolean modified = false;

        for (int i = 0; i < propertyNames.length; i++) {
            if ("creationTime".equals(propertyNames[i])) {
                Long ctime = (Long)state[i];
                if (ctime == null || ctime.longValue() == 0) {
                    state[i] = new Long(System.currentTimeMillis());
                    modified =  true;
                }
            } else if ("modifiedTime".equals(propertyNames[i])) {
                state[i] = new Long(System.currentTimeMillis());
                modified =  true;
            }
        }
        return modified;
    }
}
