package org.hyperic.hq.common.utils;

import java.util.Collection;
import java.util.List;

import org.hyperic.hibernate.PersistedObject;
import org.hyperic.util.Transformer;

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

    /**
     * helper method to add get all ids from a collection.
     */
    @SuppressWarnings("unchecked")
    public static List<Integer> getIdsFromCollection(Collection<? extends PersistedObject> objs) {
        List<Integer> ids = new Transformer<PersistedObject, Integer>() {
            @Override
            public Integer transform(PersistedObject obj) {
                return obj.getId();
            }
        }.transform((Collection<PersistedObject>) objs);
        return ids;
    }

   
}
