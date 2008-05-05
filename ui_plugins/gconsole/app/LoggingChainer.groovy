import java.io.Serializable;
import java.util.Iterator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.CallbackException;
import org.hibernate.EntityMode;
import org.hibernate.Interceptor;
import org.hibernate.Transaction;
import org.hibernate.type.Type;
import org.hyperic.hibernate.HibernateInterceptorChain;
import org.hyperic.util.Runnee;

class LoggingChainer
    implements HibernateInterceptorChain
{
    private List _log   = []
    private Map  _stats = [:]
    
    private Object logMsg(String op, String msg, Closure yield) {
        long start = System.currentTimeMillis()
        Object res = yield()
        long end = System.currentTimeMillis()
        long duration = end - start
        _log << [op: op, msg: msg, 
                 start: start, duration: duration]
        
        Map stat = _stats.get(op, [num: 0, min: duration, max: duration,
                                   minTimeStamp: start, maxTimeStamp: start])
        stat.num++
        if (stat.min > duration) {
            stat.min          = duration
            stat.minTimeStamp = start
        }
        if (stat.max < duration) {
            stat.max          = duration
            stat.maxTimeStamp = start
        }
        
        return res
    }
    
    public List getLogs() {
        return _log
    }
    
    public Map getStats() {
        return _stats
    }
    
    public void afterTransactionBegin(HibernateInterceptorChain next, 
                                      Interceptor target, Transaction tx) 
    {
        logMsg("afterTxBegin", '') {
            next.afterTransactionBegin(null, target, tx)
        }
    }

    public void afterTransactionCompletion(HibernateInterceptorChain next, 
                                           Interceptor target, Transaction tx) 
    {
        logMsg('afterTxComplete', '') {
            next.afterTransactionCompletion(null, target, tx)
        }
    }

    public void beforeTransactionCompletion(HibernateInterceptorChain next, 
                                            Interceptor target, Transaction tx) 
    {
        logMsg('beforeTxComplete', '') {
            next.beforeTransactionCompletion(null, target, tx)
        }
    }

    public int[] findDirty(HibernateInterceptorChain next, Interceptor target, 
                           Object entity, Serializable id, 
                           Object[] currentState, Object[] previousState, 
                           String[] propertyNames, Type[] types) 
    {
        logMsg('findDirty', "entity=${entity} id=${id}") {
            next.findDirty(null, target, entity, id,currentState, previousState,  
                           propertyNames, types)
        }
    }

    public Object getEntity(HibernateInterceptorChain next, Interceptor target, 
                            String entityName, Serializable id) 
    {
        logMsg('getEntity', "entity=${entityName} id=${id}") {
            next.getEntity(null, target, entityName, id);
        }
    }

    public String getEntityName(HibernateInterceptorChain next, 
                                Interceptor target, Object object) 
    {
        logMsg('getEntityName', "object=${object}") {
            next.getEntityName(null, target, object);
        }
    }

    public Object instantiate(HibernateInterceptorChain next, 
                              Interceptor target, String entityName, 
                              EntityMode entityMode, Serializable id) 
    {
        logMsg('instantiate', "entityName=${entityName} mode=${entityMode} id=${id}") {
            next.instantiate(null, target, entityName, entityMode, id)
        }
    }

    public Boolean isTransient(HibernateInterceptorChain next, 
                               Interceptor target, Object entity) 
    {
        logMsg('isTransient', "entity=${entity}") {
            return next.isTransient(null, target, entity);
        }
    }

    public void onCollectionRecreate(HibernateInterceptorChain next, 
                                     Interceptor target, Object collection, 
                                     Serializable key) 
        throws CallbackException 
    {
        logMsg("onCollectionRecreate", "key=${key}") {
            next.onCollectionRecreate(null, target, collection, key)
        }
    }

    public void onCollectionRemove(HibernateInterceptorChain next, 
                                   Interceptor target, Object collection, 
                                   Serializable key) 
        throws CallbackException 
    {
        logMsg("onCollectionRemove", "key=${key}") {
            next.onCollectionRemove(null, target, collection, key)
        }
    }

    public void onCollectionUpdate(HibernateInterceptorChain next, 
                                   Interceptor target, Object collection, 
                                   Serializable key) 
        throws CallbackException 
    {
        logMsg("onCollectionUpdate", "key=${key}") {
            next.onCollectionUpdate(null, target, collection, key)
        }
    }

    public void onDelete(HibernateInterceptorChain next, Interceptor target, 
                         Object entity, Serializable id, Object[] state, 
                         String[] propertyNames, Type[] types) 
    {
        logMsg('onDelete', "entity=${entity} id=${id}") {
            next.onDelete(null, target, entity, id, state, propertyNames, types)
        }
    }

    public boolean onFlushDirty(HibernateInterceptorChain next, 
                                Interceptor target, Object entity, 
                                Serializable id, Object[] currentState, 
                                Object[] previousState, String[] propertyNames, 
                                Type[] types) 
    {
        logMsg('onFlushDirty', "entity=${entity} id=${id}") {
            next.onFlushDirty(null, target, entity, id, currentState, 
                              previousState,  propertyNames, types)
        }
    }

    public boolean onLoad(HibernateInterceptorChain next, Interceptor target, 
                          Object entity, Serializable id, Object[] state, 
                          String[] propertyNames, Type[] types) 
    {
        logMsg('onLoad', "${id}") {
            next.onLoad(null, target, entity, id, state, propertyNames, types)
        }
    }

    public String onPrepareStatement(HibernateInterceptorChain next, 
                                     Interceptor target, String sql) 
    {
        logMsg('onPrepareStatement', sql) {
            next.onPrepareStatement(null, target, sql)
        }
    }

    public boolean onSave(HibernateInterceptorChain next, Interceptor target, 
                          Object entity, Serializable id, Object[] state, 
                          String[] propertyNames, Type[] types) 
    {
        logMsg('onSave', "entity=$entity id=$id") {
            next.onSave(null, target, entity, id, state,propertyNames, types)
        }
    }

    public void postFlush(HibernateInterceptorChain next, Interceptor target, 
                          Iterator entities) 
    {
        logMsg("postFlush", "numEntities = ${entities.size()}") {
            next.postFlush(null, target, entities);
        }
    }

    public void preFlush(HibernateInterceptorChain next, Interceptor target, 
                         Iterator entities) 
    {
        logMsg('preFlush', "numEntities = ${entities.size()}") {
            next.preFlush(null, target, entities)
        }
    }
}
