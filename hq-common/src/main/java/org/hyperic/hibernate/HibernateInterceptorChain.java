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

public interface HibernateInterceptorChain {
    void onDelete(HibernateInterceptorChain next, Interceptor target, 
                  Object entity, Serializable id,  
                  Object[] state, String[] propertyNames,  
                  Type[] types);

    boolean onFlushDirty(HibernateInterceptorChain next, Interceptor target, 
                         Object entity, Serializable id, Object[] currentState,   
                         Object[] previousState, String[] propertyNames,  
                         Type[] types);

    boolean onLoad(HibernateInterceptorChain next,Interceptor target, 
                   Object entity, Serializable id, Object[] state,   
                   String[] propertyNames, Type[] types);
    
    boolean onSave(HibernateInterceptorChain next,Interceptor target, 
                   Object entity, Serializable id, Object[] state,    
                   String[] propertyNames, Type[] types); 

    void postFlush(HibernateInterceptorChain next, Interceptor target, 
                   Iterator entities);
    void preFlush(HibernateInterceptorChain next, Interceptor target, 
                  Iterator entities);

    Boolean isTransient(HibernateInterceptorChain next, Interceptor target, 
                        Object entity);

    Object instantiate(HibernateInterceptorChain next, Interceptor target, 
                       String entityName, EntityMode entityMode, 
                       Serializable id); 

    int[] findDirty(HibernateInterceptorChain next, Interceptor target, 
                    Object entity, Serializable id,
                    Object[] currentState, Object[] previousState,
                    String[] propertyNames, Type[] types);

    String getEntityName(HibernateInterceptorChain next, Interceptor target, 
                         Object object);

    Object getEntity(HibernateInterceptorChain next, Interceptor target, 
                     String entityName, Serializable id);

    void afterTransactionBegin(HibernateInterceptorChain next, 
                               Interceptor target, Transaction tx);
    void afterTransactionCompletion(HibernateInterceptorChain next, 
                                    Interceptor target, Transaction tx);
    void beforeTransactionCompletion(HibernateInterceptorChain next,
                                     Interceptor target, Transaction tx);

    String onPrepareStatement(HibernateInterceptorChain next,
                              Interceptor target, String sql);

    void onCollectionRemove(HibernateInterceptorChain next,
                            Interceptor target, Object collection, 
                            Serializable key) 
        throws CallbackException;

    void onCollectionRecreate(HibernateInterceptorChain next, 
                              Interceptor target, Object collection, 
                              Serializable key) 
        throws CallbackException;

    void onCollectionUpdate(HibernateInterceptorChain next,
                            Interceptor target, Object collection, 
                            Serializable key) 
        throws CallbackException;
}
