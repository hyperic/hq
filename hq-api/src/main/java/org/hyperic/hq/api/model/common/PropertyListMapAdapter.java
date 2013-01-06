package org.hyperic.hq.api.model.common;
 
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.hyperic.hq.api.model.PropertyList;

public class PropertyListMapAdapter extends XmlAdapter<PropertyListMapElements[], Map<String, PropertyList>> {
    public PropertyListMapElements[] marshal(Map<String, PropertyList> map) throws Exception {
        if (map==null) {
            return null;
        }
        PropertyListMapElements[] mapElements = new PropertyListMapElements[map.size()];
        int i = 0;
        for (Map.Entry<String, PropertyList> entry : map.entrySet()) {
            mapElements[i++] = new PropertyListMapElements(entry.getKey(), entry.getValue());
        }

        return mapElements;
    }

    public Map<String, PropertyList> unmarshal(PropertyListMapElements[] displayMap) throws Exception {
        Map<String, PropertyList> r = new HashMap<String, PropertyList>();
        if (displayMap!=null) {
            for (PropertyListMapElements mapelement : displayMap) {
                r.put(mapelement.key, mapelement.value);
            }
        }
        return r;
    }
}
