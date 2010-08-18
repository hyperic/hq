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

package org.hyperic.hibernate;

import java.io.Serializable;
import java.util.Iterator;

import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

public class DefaultInterceptorChain 
    implements HibernateInterceptorChain 
{
    public void onDelete(HibernateInterceptorChain chain, Interceptor target, 
                         Object entity, Serializable id,  
                         Object[] state, String[] propertyNames,  
                         Type[] types) 
    {
        target.onDelete(entity, id, state, propertyNames, types);
    }

    public boolean onFlushDirty(HibernateInterceptorChain chain, 
                                Interceptor target, Object entity, 
                                Serializable id, Object[] currentState,  
                                Object[] previousState, String[] propertyNames,  
                                Type[] types) 
    {
        return target.onFlushDirty(entity, id, currentState, previousState, 
                                   propertyNames, types);
    }

    public boolean onLoad(HibernateInterceptorChain chain, Interceptor target, 
                          Object entity, Serializable id, Object[] state,   
                          String[] propertyNames, Type[] types)
    {
        return target.onLoad(entity, id, state, propertyNames, types);
    }
    
    public boolean onSave(HibernateInterceptorChain chain, Interceptor target, 
                          Object entity, Serializable id, Object[] state,    
                          String[] propertyNames, Type[] types)
    {
        return target.onSave(entity, id, state, propertyNames, types);
    }

    public void postFlush(HibernateInterceptorChain chain, Interceptor target, 
                          Iterator entities) 
    {
        target.postFlush(entities);
    }
    
    public void preFlush(HibernateInterceptorChain chain, Interceptor target, 
                         Iterator entities) 
    {
        target.preFlush(entities);
    }

    public Boolean isTransient(HibernateInterceptorChain chain, 
                               Interceptor target, Object entity) 
    {
        return target.isTransient(entity);
    }

    public Object instantiate(HibernateInterceptorChain chain,
                              Interceptor target, String entityName, 
                              EntityMode entityMode, Serializable id)
    {
        return target.instantiate(entityName, entityMode, id);
    }

    public int[] findDirty(HibernateInterceptorChain chain, Interceptor target, 
                           Object entity, Serializable id,
                           Object[] currentState, Object[] previousState,                           
                           String[] propertyNames, Type[] types)
    {
        return target.findDirty(entity, id, currentState, previousState, 
                                propertyNames, types);
    }
    
    
    public String getEntityName(HibernateInterceptorChain chain, 
                                Interceptor target, Object object) 
    {
        return target.getEntityName(object);
    }

    public Object getEntity(HibernateInterceptorChain chain, Interceptor target, 
                            String entityName, Serializable id) 
    {
        return target.getEntity(entityName, id);
    }

    public void afterTransactionBegin(HibernateInterceptorChain chain, 
                                      Interceptor target, Transaction tx) 
    {
        target.afterTransactionBegin(tx);
    }
    
    public void afterTransactionCompletion(HibernateInterceptorChain chain,
                                           Interceptor target, Transaction tx) 
    {
        target.afterTransactionCompletion(tx);
    }
    
    public void beforeTransactionCompletion(HibernateInterceptorChain chain,
                                            Interceptor target, Transaction tx)
    {
        target.beforeTransactionCompletion(tx);
    }

    public String onPrepareStatement(HibernateInterceptorChain chain, 
                                     Interceptor target, String sql) 
    {
        return target.onPrepareStatement(sql);
    }

    public void onCollectionRemove(HibernateInterceptorChain chain, 
                                   Interceptor target, Object collection, 
                                   Serializable key) 
        throws CallbackException
    {
        target.onCollectionRemove(collection, key);
    }

    public void onCollectionRecreate(HibernateInterceptorChain chain, 
                                     Interceptor target, Object collection, 
                                     Serializable key) 
        throws CallbackException
    {
        target.onCollectionRecreate(collection, key);
    }

    public void onCollectionUpdate(HibernateInterceptorChain chain,
                                   Interceptor target, Object collection, 
                                   Serializable key) 
        throws CallbackException
    {
        target.onCollectionUpdate(collection, key);
    }
}
