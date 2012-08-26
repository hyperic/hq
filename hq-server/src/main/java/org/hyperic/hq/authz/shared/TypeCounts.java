package org.hyperic.hq.authz.shared;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.hyperic.hq.util.Reference;

public class TypeCounts {
    
    private final Map<Integer, Reference<Integer>> appdefTypeCount = new HashMap<Integer, Reference<Integer>>();
    private final Map<Integer, Map<Integer, Reference<Integer>>> protoTypeCount = new HashMap<Integer, Map<Integer, Reference<Integer>>>();

    public TypeCounts() {}

    public void incrementProtoTypeCount(Integer appdefTypeId, Integer protoTypeId) {
        if (protoTypeId == null) {
            return;
        }
        Map<Integer, Reference<Integer>> protoTypesPerAppdefType = protoTypeCount.get(appdefTypeId);
        if (protoTypesPerAppdefType == null) {
            protoTypesPerAppdefType = new HashMap<Integer, Reference<Integer>>();
            protoTypeCount.put(appdefTypeId, protoTypesPerAppdefType);
        }
        increment(protoTypesPerAppdefType, protoTypeId);
    }

    public void incrementAppdefTypeCount(Integer appdefTypeId) {
        if (appdefTypeId == null) {
            return;
        }
        increment(appdefTypeCount, appdefTypeId);
    }

    private void increment(Map<Integer, Reference<Integer>> typeMap, Integer typeId) {
        Reference<Integer> count = typeMap.get(typeId);
        if (count == null) {
            count = new Reference<Integer>(1);
            typeMap.put(typeId, count);
        } else {
            count.set(count.get()+1);
        }
    }
    
    public Map<Integer, Reference<Integer>> getProtoTypeCounts (int appdefTypeId) {
        Map<Integer, Reference<Integer>> map = protoTypeCount.get(appdefTypeId);
        if (map == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(map);
    }
    
    public Map<Integer, Reference<Integer>> getAppdefTypeCounts () {
        return Collections.unmodifiableMap(appdefTypeCount);
    }

}
