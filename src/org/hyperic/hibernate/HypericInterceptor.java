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
