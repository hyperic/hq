package org.hyperic.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class Classifier<A,Y,Z> {

    public Map<Y,Z> classifyUnique(Collection<A> c) {
        if (c == null || c.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<Y,Z> rtn = new HashMap<Y,Z>();
        for (final A o : c) {
            if (o == null) {
                continue;
            }
            final NameValue<Y,Z> nv= classify(o);
            if (nv == null) {
                continue;
            }
            rtn.put(nv.key, nv.val);
        }
        return rtn;
    }
    
    public Map<Y,Set<Z>> classifyUniqueValues(Collection<A> c) {
        if (c == null || c.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<Y, Set<Z>> rtn = new HashMap<Y, Set<Z>>();
        for (final A o : c) {
            if (o == null) {
                continue;
            }
            final NameValue<Y,Z> nv = classify(o);
            if (nv == null) {
                continue;
            }
            Set<Z> tmp;
            if (null == (tmp = rtn.get(nv.key))) {
                tmp = new HashSet<Z>();
                rtn.put(nv.key, tmp);
            }
            tmp.add(nv.val);
        }
        return rtn;
    }
    
    public Map<Y,Collection<Z>> classify(Collection<A> c) {
        if (c == null || c.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<Y, Collection<Z>> rtn = new HashMap<Y, Collection<Z>>();
        for (final A o : c) {
            if (o == null) {
                continue;
            }
            final NameValue<Y,Z> nv = classify(o);
            if (nv == null) {
                continue;
            }
            Collection<Z> tmp;
            if (null == (tmp = rtn.get(nv.key))) {
                tmp = new ArrayList<Z>();
                rtn.put(nv.key, tmp);
            }
            tmp.add(nv.val);
        }
        return rtn;
    }
    
    public abstract NameValue<Y,Z> classify(A key);
    
    public class NameValue<Y,Z> {
        private final Y key;
        private final Z val;
        public NameValue(Y key, Z val) {
            this.key = key;
            this.val = val;
        }
    }

}
