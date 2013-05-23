package org.hyperic.hq.util.properties;

public class PropertiesUtil {

    public static Boolean getBooleanValue(String property) {
        if ((null == property) || property.isEmpty()) {
            return null;
        }
        if (property.equalsIgnoreCase("true") || property.equalsIgnoreCase("yes")) {
            return true;
        }
        if (property.equalsIgnoreCase("false") || property.equalsIgnoreCase("no")) {
            return false;
        }
        return null;
    }

}
