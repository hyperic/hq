package org.hyperic.hq.common.utils;

import java.util.Collection;

import org.hyperic.hibernate.PersistedObject;

public abstract class PersistedObjectHelper {
    
    
    public static String getIdsStrFromCollection(Collection<? extends PersistedObject> objs) {
        StringBuilder rtn = new StringBuilder(objs.size() * 8);
        rtn.append("(");
        for(PersistedObject obj :objs) {
            rtn.append(obj.getId())
            .append(",");
        }
        if (!objs.isEmpty()) {
            rtn.deleteCharAt(rtn.length() - 1);
        }
        rtn.append(")");
        return rtn.toString();
    }
   
}
