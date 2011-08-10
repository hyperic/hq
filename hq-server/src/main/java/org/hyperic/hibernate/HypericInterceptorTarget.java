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
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.ObjectNotFoundException;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

/**
 * multi-purpose interceptor for injecting runtime logic,
 *
 * One use case is to set creation and modified time on
 * on save, merge or collection cascades
 */
@SuppressWarnings("serial")
public class HypericInterceptorTarget extends EmptyInterceptor {
    private final Log _log = LogFactory.getLog(HypericInterceptor.class);
        
    public String onPrepareStatement(String sql) {
        return sql;
    }

    private boolean entHasContainerManagedTimestamp(Object o) {
        return o instanceof ContainerManagedTimestampTrackable;
    }
    
    public boolean onFlushDirty(Object entity, Serializable id, 
                                Object[] currentState, Object[] previousState, 
                                String[] propertyNames, Type[] types) {
        try {
            if (entHasContainerManagedTimestamp(entity)) {
                return updateTimestamp((ContainerManagedTimestampTrackable)entity, 
                                       currentState, previousState, propertyNames);
            }
        } catch (ObjectNotFoundException e) {
            _log.warn("entity not found: " + e);
            _log.debug(e,e);
        }
        return false;
    }

    public boolean onSave(Object entity, Serializable id, Object[] state, 
                          String[] propertyNames, Type[] types) {
        try {
            if (entHasContainerManagedTimestamp(entity)) {
                return updateTimestamp((ContainerManagedTimestampTrackable)entity, 
                                       state, null, propertyNames);
            }
        } catch (ObjectNotFoundException e) {
            _log.warn("entity not found: " + e);
            _log.debug(e,e);
        }
        return false;
    }

    private boolean updateTimestamp(ContainerManagedTimestampTrackable entity, 
                                    Object[] curState, Object[] prevState, 
                                    String[] propertyNames) {
        boolean modified = false;
        long ts = System.currentTimeMillis();
        int modifiedIdx = -1;
        int createdIdx = -1;
        int modifiedIdxToCheckMTime = -1;
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
            if (("creationTime".equals(propertyNames[i]) ||
                "ctime".equals(propertyNames[i])) && 
                entity.allowContainerManagedCreationTime()) {
                Long ctime = (Long)curState[i];
                if (ctime == null || ctime.longValue() == 0) {
                    createdIdx = i;
                    modified =  true;
                }
            } else if ("modifiedTime".equals(propertyNames[i]) ||
                       "mtime".equals(propertyNames[i])) {
                if (entity.allowContainerManagedLastModifiedTime()) {
                    modified = true;                    
                    modifiedIdx = i;  
                } else {
                    modifiedIdxToCheckMTime = i;
                }
            }
        }
        if (createdIdx >= 0) {
            curState[createdIdx] = new Long(ts);
        }
        if (modifiedIdx >= 0 && modified) {
            curState[modifiedIdx] = new Long(ts);
        }
        
        enforceMTimeAtLeastCTime(curState, modifiedIdxToCheckMTime, createdIdx);   
        
        return modified;
    }

    /**
     * The mtime must be at least equal to the ctime. Otherwise, set 
     * the mtime to the ctime. This may be an issue when the mtime is 
     * managed explicitly, but the ctime is managed by the container.
     * 
     * @param curState
     * @param modifiedIdx
     * @param createdIdx
     */
    private void enforceMTimeAtLeastCTime(Object[] curState, 
                                          int modifiedIdx,
                                          int createdIdx) {
        if (createdIdx >= 0 && modifiedIdx >= 0) {
            Long ctime = (Long)curState[createdIdx];
            Long mtime = (Long)curState[modifiedIdx];
            
            if (ctime != null && mtime != null && mtime.longValue() < ctime.longValue()) {
                curState[modifiedIdx] = ctime;
            }
        }
    }

    public void afterTransactionBegin(Transaction tx) {
        super.afterTransactionBegin(tx);
    }

    public void afterTransactionCompletion(Transaction tx) {
        super.afterTransactionCompletion(tx);
    }

    public void beforeTransactionCompletion(Transaction tx) {
        super.beforeTransactionCompletion(tx);
    }

    public int[] findDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        return super.findDirty(entity, id, currentState, previousState, propertyNames,
                               types);
    }

    public Object getEntity(String entityName, Serializable id) {
        return super.getEntity(entityName, id);
    }

    public String getEntityName(Object object) {
        return super.getEntityName(object);
    }

    public Object instantiate(String entityName, EntityMode entityMode, Serializable id) {
        return super.instantiate(entityName, entityMode, id);
    }

    public Boolean isTransient(Object entity) {
        return super.isTransient(entity);
    }

    public void onCollectionRecreate(Object collection, Serializable key) throws CallbackException {
        super.onCollectionRecreate(collection, key);
    }

    public void onCollectionRemove(Object collection, Serializable key) throws CallbackException {
        super.onCollectionRemove(collection, key);
    }

    public void onCollectionUpdate(Object collection, Serializable key) throws CallbackException {
        super.onCollectionUpdate(collection, key);
    }

    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        super.onDelete(entity, id, state, propertyNames, types);
    }

    public boolean onLoad(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        return super.onLoad(entity, id, state, propertyNames, types);
    }

    public void postFlush(Iterator entities) {
        super.postFlush(entities);
    }

    public void preFlush(Iterator entities) {
        super.preFlush(entities);
    }
}
