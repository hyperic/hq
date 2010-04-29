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

package org.hyperic.hq.application;

import java.io.Serializable;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.hyperic.hibernate.DefaultInterceptorChain;
import org.hyperic.hibernate.HibernateInterceptorChain;
import org.hyperic.util.Runnee;
import org.springframework.stereotype.Component;
@Component
class HQHibernateLogger 
    implements HibernateInterceptorChain, HibernateLogManager
{
    private final Log _log = LogFactory.getLog(HQHibernateLogger.class);
    private final HibernateInterceptorChain _defaultChain;
    
    private ThreadLocal _listeners = new ThreadLocal(); 
    
    public HQHibernateLogger() {
        _defaultChain = new DefaultInterceptorChain();
    }
    
    public void log(HibernateInterceptorChain chain, Runnee r) 
        throws Exception 
    {
        _listeners.set(chain);
        try {
            r.run();
        } finally {
            _listeners.remove();
        }
    }

    private HibernateInterceptorChain getCurrent() { 
        HibernateInterceptorChain chain = 
            (HibernateInterceptorChain)_listeners.get();
        
        if (chain != null)
            return chain;
        
        return _defaultChain;
    }
    
    private HibernateInterceptorChain getNext(HibernateInterceptorChain next) {
        return _defaultChain;
    }

    public void afterTransactionBegin(HibernateInterceptorChain next, 
                                      Interceptor target, Transaction tx) 
    {
        getCurrent().afterTransactionBegin(_defaultChain, target, tx);
    }

    public void afterTransactionCompletion(HibernateInterceptorChain next, 
                                           Interceptor target, Transaction tx) 
    {
        getCurrent().afterTransactionCompletion(_defaultChain, target, tx);
    }

    public void beforeTransactionCompletion(HibernateInterceptorChain next, 
                                            Interceptor target, Transaction tx) 
    {
        getCurrent().beforeTransactionCompletion(_defaultChain, target, tx);
    }

    public int[] findDirty(HibernateInterceptorChain next, Interceptor target, 
                           Object entity, Serializable id, 
                           Object[] currentState, Object[] previousState, 
                           String[] propertyNames, Type[] types) 
    {
        return getCurrent().findDirty(_defaultChain, target, entity, id, 
                                      currentState, previousState, 
                                      propertyNames, types);
    }

    public Object getEntity(HibernateInterceptorChain next, Interceptor target, 
                            String entityName, Serializable id) 
    {
        return getCurrent().getEntity(_defaultChain, target, entityName, id);
    }

    public String getEntityName(HibernateInterceptorChain next, 
                                Interceptor target, Object object) 
    {
        return getCurrent().getEntityName(_defaultChain, target, object);
    }

    public Object instantiate(HibernateInterceptorChain next, 
                              Interceptor target, String entityName, 
                              EntityMode entityMode, Serializable id) 
    {
        return getCurrent().instantiate(_defaultChain, target, entityName, 
                                        entityMode, id);
    }

    public Boolean isTransient(HibernateInterceptorChain next, 
                               Interceptor target, Object entity) 
    {
        return getCurrent().isTransient(_defaultChain, target, entity);
    }

    public void onCollectionRecreate(HibernateInterceptorChain next, 
                                     Interceptor target, Object collection, 
                                     Serializable key) 
        throws CallbackException 
    {
        getCurrent().onCollectionRecreate(_defaultChain, target, 
                                          collection, key);
    }

    public void onCollectionRemove(HibernateInterceptorChain next, 
                                   Interceptor target, Object collection, 
                                   Serializable key) 
        throws CallbackException 
    {
        getCurrent().onCollectionRemove(_defaultChain, target, collection, 
                                        key);
    }

    public void onCollectionUpdate(HibernateInterceptorChain next, 
                                   Interceptor target, Object collection, 
                                   Serializable key) 
        throws CallbackException 
    {
        getCurrent().onCollectionUpdate(_defaultChain, target, collection, key);
    }

    public void onDelete(HibernateInterceptorChain next, Interceptor target, 
                         Object entity, Serializable id, Object[] state, 
                         String[] propertyNames, Type[] types) 
    {
        getCurrent().onDelete(_defaultChain, target, entity, id, state, 
                              propertyNames, types); 
    }

    public boolean onFlushDirty(HibernateInterceptorChain next, 
                                Interceptor target, Object entity, 
                                Serializable id, Object[] currentState, 
                                Object[] previousState, String[] propertyNames, 
                                Type[] types) 
    {
        return getCurrent().onFlushDirty(_defaultChain, target, entity, id, 
                                         currentState, previousState, 
                                         propertyNames, types);
    }

    public boolean onLoad(HibernateInterceptorChain next, Interceptor target, 
                          Object entity, Serializable id, Object[] state, 
                          String[] propertyNames, Type[] types) 
    {
        return getCurrent().onLoad(_defaultChain, target, entity, id, state, 
                                   propertyNames, types);
    }

    public String onPrepareStatement(HibernateInterceptorChain next, 
                                     Interceptor target, String sql) 
    {
        return getCurrent().onPrepareStatement(_defaultChain, target, sql);
    }

    public boolean onSave(HibernateInterceptorChain next, Interceptor target, 
                          Object entity, Serializable id, Object[] state, 
                          String[] propertyNames, Type[] types) 
    {
        return getCurrent().onSave(_defaultChain, target, entity, id, state, 
                                   propertyNames, types);
    }

    public void postFlush(HibernateInterceptorChain next, Interceptor target, 
                          Iterator entities) 
    {
        getCurrent().postFlush(_defaultChain, target, entities);
    }

    public void preFlush(HibernateInterceptorChain next, Interceptor target, 
                         Iterator entities) 
    {
        getCurrent().preFlush(_defaultChain, target, entities);
    }
}
