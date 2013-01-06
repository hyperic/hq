package org.hyperic.hq.api.model.common;

import java.util.HashMap;
import java.util.Map;
  
import javax.xml.bind.annotation.adapters.XmlAdapter;

public class MapPropertiesAdapter extends XmlAdapter<PropertyMapElements[], Map<String, String>> {
    public PropertyMapElements[] marshal(Map<String, String> map) throws Exception {
        if (map==null) {
            return null;
        }
        PropertyMapElements[] mapElements = new PropertyMapElements[map.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            mapElements[i++] = new PropertyMapElements(entry.getKey(), entry.getValue());
        }

        return mapElements;
    }

    public Map<String, String> unmarshal(PropertyMapElements[] displayMap) throws Exception {
        Map<String, String> r = new HashMap<String, String>();
        if (displayMap!=null) {
            for (PropertyMapElements mapelement : displayMap) {
                r.put(mapelement.key, mapelement.value);
            }
        }
        return r;
    }
}
