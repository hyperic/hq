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

package org.hyperic.hq.product.jmx;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.util.config.ConfigSchema;

public abstract class MxQuery {
    public static final String PROP_OBJECT_NAME    = "OBJECT_NAME";
    public static final String PROP_ATTRIBUTE_NAME = "ATTRIBUTE_NAME";
    public static final String PROP_MBEAN_CLASS    = "MBEAN_CLASS";
    public static final String PROP_OBJECT_NAME_FILTER = "OBJECT_NAME_FILTER";

    protected static final Properties EMPTY_PROPERTIES = new Properties();
    protected static final Log log = LogFactory.getLog(MxQuery.class);

    private Map attrs = new HashMap();
    private String name;
    private String url = null;
    private ServerDetector detector = null;
    protected ObjectName objectName;
    private Properties cprops;

    public void initialize() {
        
    }

    void setObjectName(ObjectName objectName) {
        this.objectName = objectName;
    }
    
    public ObjectName getObjectName() {
        return objectName;
    }

    public Properties getCustomProperties() {
        return this.cprops;
    }

    public String[] getAttributeNames() {
        String type = getResourceType();
        ConfigSchema schema =
            this.getServerDetector().getCustomPropertiesSchema(type);

        return schema.getOptionNames();
    }

    public void getAttributes(MBeanServerConnection mServer)
        throws PluginException {

        String[] names = getAttributeNames();

        if (names.length == 0) {
            this.cprops = EMPTY_PROPERTIES;
            return;
        }

        for (int i=0; i<names.length; i++) {
            String name = names[i];
            ObjectName objName;

            int ix = name.lastIndexOf(':');
            if (ix == -1) {
                //"serverVersion"
                objName = getObjectName();
            }
            else {
                //domain:Key=Val:serverVersion
                try {
                    objName = new ObjectName(name.substring(0, ix));
                } catch (MalformedObjectNameException e) {
                    throw new PluginException(name, e);
                }
                name = name.substring(ix+1);
            }

            Object value;
            try {
                value = mServer.getAttribute(objName, name);
            } catch (Exception e) {
                log.debug("getAttribute(" + objName + ", " + name + ") failed", e);
                continue;
            }
            if (value != null) {
                String stringValue;
                if (value instanceof Object[]) {
                    stringValue =
                        Arrays.asList((Object[])value).toString();
                }
                else {
                    stringValue = value.toString();
                }
                setAttribute(names[i], stringValue);
            } else {
                // Show 'Attribute was null' in UI.
                log.debug("getAttribute(" + objName + ", " + name + ") returned null");
                setAttribute(names[i], "Attribute was null");
            }
        }

        this.cprops = new Properties();
        this.cprops.putAll(this.attrs);
    }

    public void setURL(String url) {
        this.url = url;
    }

    public String getURL() {
        if (this.url == null) {
            return this.getParent().getURL();
        }
        return this.url;
    }
    
    public void setServerDetector(ServerDetector detector) {
        this.detector = detector;
    }
    
    public ServerDetector getServerDetector() {
        if (this.detector == null) {
            return this.getParent().getServerDetector();
        }
        return this.detector;
    }

    public void getAttributes(MBeanServerConnection mServer,
                              ObjectName name,
                              String[] attrs)
        throws PluginException {

        if (attrs.length == 0) {
            return;
        }

        AttributeList mBeanAttrs;

        try {
            mBeanAttrs = mServer.getAttributes(name, attrs);
        } catch (RemoteException e) {
            throw new PluginException("Cannot connect to server", e);
        } catch (InstanceNotFoundException e) {
            throw new PluginException("Cannot find MBean [" +
                                      name + "]", e);
        } catch (ReflectionException e) {
            throw new PluginException("MBean reflection exception", e);
        } catch (IOException e) {
            throw new PluginException("Cannot connect to server", e);
        }

        for (Iterator it=mBeanAttrs.iterator(); it.hasNext();) {
            Attribute attr = (Attribute)it.next();
            Object value = attr.getValue();
            if (value != null) {
                setAttribute(attr.getName(), value.toString());
            }
        }
    }

    public String getAttribute(String name) {
        return (String) attrs.get(name);
    }

    public String getAttribute(String name, String defval) {
        String attr = getAttribute(name);
        if (attr == null) {
            return defval;
        }
        return attr;
    }

    public void setAttribute(String name, String val) {
        this.attrs.put(name, val);
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public abstract String getQualifiedName();

    public abstract String getResourceType();

    public abstract String getIdentifier();

    public MxQuery getParent() {
        // services should override
        return null;
    }

    public boolean hasControl() {
        return true;
    }

    public Properties getResourceConfig() {
        return EMPTY_PROPERTIES;
    }

    public Properties getControlConfig() {
        return EMPTY_PROPERTIES;
    }
}
