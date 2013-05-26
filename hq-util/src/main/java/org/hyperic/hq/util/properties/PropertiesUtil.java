package org.hyperic.hq.util.properties;

public class PropertiesUtil {

    public static Boolean getBooleanValue(String property, boolean defaultValue) {
        if ((null == property) || property.equalsIgnoreCase("")) {
            return defaultValue;
        }
        if (property.equalsIgnoreCase("true") || property.equalsIgnoreCase("yes")) {
            return true;
        }
        if (property.equalsIgnoreCase("false") || property.equalsIgnoreCase("no")) {
            return false;
        }
        return defaultValue;
    }

}
