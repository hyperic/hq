package org.hyperic.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Transformer<T, R> {
    
    public Set<R> transformToSet(Collection<T> c) {
        if (c == null || c.isEmpty()) {
            return Collections.emptySet();
        }
        final Set<R> rtn = new HashSet<R>();
        for (T obj : c) {
            R t = transform(obj);
            if (t != null) {
                rtn.add(t);
            }
        }
        return rtn;
    }
    
    public List<R> transform(Collection<T> c) {
        if (c == null || c.isEmpty()) {
            return Collections.emptyList();
        }
        final List<R> rtn = new ArrayList<R>(c.size());
        for (T obj : c) {
            R t = transform(obj);
            if (t != null) {
                rtn.add(t);
            }
        }
        return rtn;
    }
    
    public abstract R transform(T obj);

}
