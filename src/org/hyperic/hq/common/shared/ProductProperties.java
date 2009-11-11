/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.hq.common.shared;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import java.util.Enumeration;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ProductProperties {
    private static final String PROP_VERSION    = "version";
    private static final String PROP_BUILD      = "build.number";
    private static final String PROP_COMMENT    = "build.comment";
    private static final String PROP_BUILD_DATE = "build.date";
    private static final String PROP_FLAVOUR    = "hq.flavour";
    private static final String PROP_ISDEV      = "hq.isDev";

    private static final Log  _log = 
        LogFactory.getLog(ProductProperties.class);

    private static       Properties _props;
    private static final Object     _propsLock = new Object();

    private ProductProperties(){}
 
    public static String getVersion() {
        return getRequiredProperty(PROP_VERSION);
    }

    public static String getBuild() {
        return getRequiredProperty(PROP_BUILD);
    }

    public static String getComment() {
        return getRequiredProperty(PROP_COMMENT);
    }

    public static String getBuildDate() {
        return getRequiredProperty(PROP_BUILD_DATE);
    }

    public static String getFlavour() {
        return getRequiredProperty(PROP_FLAVOUR);
    }

    public static boolean isDev() {
        return Boolean.valueOf(getRequiredProperty(PROP_ISDEV)).booleanValue();
    }

    private static void load(String name, boolean required) {
        //XXX unhardcode these filenames here and elsewhere
        final String[] jars = { //we should find one or the other
            "hq-product.jar",   //agent side (including command-line)
            "hq.jar"            //server side
        };
        ClassLoader loader = ProductProperties.class.getClassLoader();
        InputStream in = null;
        try { //XXX must be better way other than renaming these files w/ an hq- prefix?
            //HHQ-3528 make sure we find {version,product}.properties in the right place(s)
            //also note AgentCommandsService.upgrade has its own way but only works for the agent
            for (Enumeration urls = loader.getResources(name); urls.hasMoreElements(); ) {
                URL url = (URL)urls.nextElement();
                for (int i=0; i<jars.length; i++) {
                    //example: url == file:/.../pdk/lib/hq-product.jar!/version.properties
                    if (url.getFile().endsWith(jars[i] + "!/" + name)) {
                        in = url.openStream();
                        _log.debug("Found " + name + " via " + url);
                        break;
                    }
                }
            }
        } catch (IOException e) {
            //fallthru
        }

        if (in == null) {
            //fallback to the old fashioned way
            if ((in = loader.getResourceAsStream(name)) != null) {
                _log.debug("Found " + name + " via getResourceAsStream");
            }
        }

        if (in == null) {
            if (required) {
                throw new IllegalStateException("Package not packed " + 
                                                "correctly, missing: " + name);
            }
            return;
        }

        try {
            _props.load(in);
        } catch(IOException e) {
            throw new IllegalStateException("Failed to read " + name +
                                            ": " + e);
        }    
    }
    
    public static Properties getProperties() {
        synchronized (_propsLock) {
            if (_props == null) {
                _props = new Properties();
                
                load("version.properties", true);
                load("product.properties", false);
            }
        }
        return _props;
    }

    public static String getProperty(String key) {
        return getProperties().getProperty(key);
    }
    
    private static String getRequiredProperty(String prop) {
        String res;

        if((res = getProperty(prop)) == null)
            throw new IllegalStateException("Failed to find " + prop);
        
        return res;
    }
    
    public static Object getPropertyInstance(String key) {
        String className = ProductProperties.getProperty(key);
        if (className != null) {
            try {
                _log.debug("Property " + key + " implemented by " + className);
                return Class.forName(className).newInstance();
            } catch (Exception e) {
                _log.error("Unable to instantiate " + className +
                           " for property " + key, e);
            }
        }
        return null;
    }
}
