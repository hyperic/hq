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

import org.hibernate.CallbackException;
import org.hibernate.EmptyInterceptor;
import org.hibernate.EntityMode;
import org.hibernate.Transaction;
import org.hibernate.type.Type;

import org.hyperic.hq.context.Bootstrap;

/**
 * This interceptor delegates to others in the chain.  Most of the meat
 * is in {@link HypericInterceptorTarget} 
 */
public class HypericInterceptor 
    extends EmptyInterceptor
{
    private final HibernateInterceptorChain _chainer = 
        Bootstrap.getBean(HibernateInterceptorChain.class);
        
    private final HypericInterceptorTarget _target = 
        new HypericInterceptorTarget();
    
    public String onPrepareStatement(String sql) {
        return _chainer.onPrepareStatement(null, _target, sql);
    }

    public boolean onFlushDirty(Object entity, Serializable id, 
                                Object[] currentState, Object[] previousState, 
                                String[] propertyNames, Type[] types)
    {
        return _chainer.onFlushDirty(null, _target, entity, id, currentState, 
                                     previousState, propertyNames, types);
    }

    public boolean onSave(Object entity, Serializable id, Object[] state, 
                          String[] propertyNames, Type[] types)
    {
        return _chainer.onSave(null, _target, entity, id, state, propertyNames, 
                               types);
    }

    public void afterTransactionBegin(Transaction tx) {
        _chainer.afterTransactionBegin(null, _target, tx);
    }

    public void afterTransactionCompletion(Transaction tx) {
        _chainer.afterTransactionCompletion(null, _target, tx);
    }

    public void beforeTransactionCompletion(Transaction tx) {
        _chainer.beforeTransactionCompletion(null, _target, tx);
    }

    public int[] findDirty(Object entity, Serializable id, 
                           Object[] currentState, Object[] previousState, 
                           String[] propertyNames, Type[] types) 
    {
        return _chainer.findDirty(null, _target, entity, id, currentState, 
                                  previousState, propertyNames, types); 
    }

    public Object getEntity(String entityName, Serializable id) {
        return _chainer.getEntity(null, _target, entityName, id);
    }

    public String getEntityName(Object object) {
        return _chainer.getEntityName(null, _target, object);
    }

    public Object instantiate(String entityName, EntityMode entityMode, 
                              Serializable id) 
    {
        return _chainer.instantiate(null, _target, entityName, entityMode, id);
    }

    public Boolean isTransient(Object entity) {
        return _chainer.isTransient(null, _target, entity);
    }

    public void onCollectionRecreate(Object collection, Serializable key) 
        throws CallbackException 
    {
        _chainer.onCollectionRecreate(null, _target, collection, key);
    }

    public void onCollectionRemove(Object collection, Serializable key) 
        throws CallbackException 
    {
        _chainer.onCollectionRemove(null, _target, collection, key);
    }

    public void onCollectionUpdate(Object collection, Serializable key) 
        throws CallbackException 
    {
        _chainer.onCollectionUpdate(null, _target, collection, key);
    }

    public void onDelete(Object entity, Serializable id, Object[] state, 
                         String[] propertyNames, Type[] types) 
    {
        _chainer.onDelete(null, _target, entity, id, state, propertyNames, 
                          types);
    }

    public boolean onLoad(Object entity, Serializable id, Object[] state, 
                          String[] propertyNames, Type[] types) 
    {
        return _chainer.onLoad(null, _target, entity, id, state, propertyNames, 
                               types);
    }

    public void postFlush(Iterator entities) {
        _chainer.postFlush(null, _target, entities);
    }

    public void preFlush(Iterator entities) {
        _chainer.preFlush(null, _target, entities);
    }
}
