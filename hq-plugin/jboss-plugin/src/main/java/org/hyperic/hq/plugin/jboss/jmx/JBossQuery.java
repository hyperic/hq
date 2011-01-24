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

package org.hyperic.hq.plugin.jboss.jmx;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;

public abstract class JBossQuery {
    public static final String PROP_OBJECT_NAME    = "OBJECT_NAME";
    public static final String PROP_ATTRIBUTE_NAME = "ATTRIBUTE_NAME";

    protected static final Properties EMPTY_PROPERTIES = new Properties();

    private Map attrs = new HashMap();
    private String name;
    private String url = null;
    private ServerDetector detector = null;

    public void initialize() {
        
    }

    public abstract void getAttributes(MBeanServerConnection mServer)
        throws PluginException;

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

    public JBossQuery getParent() {
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
    
    public Properties getCustomProperties() {
        return EMPTY_PROPERTIES;
    }
}
