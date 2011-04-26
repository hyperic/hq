/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.maven.HQplugins;

import java.util.Properties;

/**
 *
 * @author administrator
 */
public class SuperProps extends Properties {

    public String getProperty(String key) {
        return this.getProperty(key, null, true);
    }

    public String getProperty(String key, String defaultValue) {
        return this.getProperty(key, defaultValue, false);
    }

    private String getProperty(String key, String defaultValue, boolean req) {
        String prefix = (String) get("config.active");
        String res = null;
        if ((prefix != null) && (!key.startsWith(prefix))) {
            Object oval = super.get(prefix + "." + key);
            res = (oval instanceof String) ? (String) oval : null;
        }

        if (res == null) {
            Object oval = super.get(key);
            res = (oval instanceof String) ? (String) oval : ((defaultValue != null) ? defaultValue : null);

            if (req && (res == null)) {
                throw new IllegalArgumentException("Proprety '" + key + "' not found");
            }
        }

        return res;
    }
}
