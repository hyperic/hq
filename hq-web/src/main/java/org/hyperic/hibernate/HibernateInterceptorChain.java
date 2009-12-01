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
