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

package org.hyperic.hq.plugin.weblogic.jmx;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.hq.plugin.weblogic.WeblogicConfig;
import org.hyperic.hq.plugin.weblogic.WeblogicProductPlugin;

public class WeblogicQuery {

    protected static final String ATTR_NOTES = "Notes";
    protected Map attrs = new HashMap();
    private WeblogicQuery parent;
    private String name;
    private String version;

    private static final WeblogicQuery[] NOOP_CHILD_QUERIES = 
        new WeblogicQuery[0];
    private static final String[] NOOP_ATTRIBUTE_NAMES = new String[0];
    private static final Properties NOOP_PROPERTIES = new Properties();

    private static final Log log = LogFactory.getLog(WeblogicQuery.class);

    public WeblogicQuery cloneInstance() {
        WeblogicQuery query;

        try {
            query = (WeblogicQuery)this.getClass().newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        query.setParent(getParent());
        query.setName(getName());
        query.setVersion(getVersion());

        return query;
    }

    public String[] getAttributeNames() {
        return NOOP_ATTRIBUTE_NAMES;
    }

    public String[] getCustomPropertiesNames() {
        return NOOP_ATTRIBUTE_NAMES;
    }

    public boolean getAttributes(MBeanServer mServer,
                                 ObjectName name) {
        return getAttributes(mServer, name, getAttributeNames());
    }

    protected void logAttrFailure(ObjectName name, String[] attrNames, Exception e) {
        if (log.isDebugEnabled()) {
            String msg = "attributes " + Arrays.asList(attrNames) + " not found for '" + name+"' : "+e;
            log.debug(msg);
        }
    }

    public boolean getAttributes(MBeanServer mServer,
                                 ObjectName name,
                                 String[] attrNames) {

        if (name == null) {
            return false;
        }
        if (attrNames.length == 0) {
            setName(name.getKeyProperty("Name"));
            return true;
        }

        AttributeList list;

        try {
            list = mServer.getAttributes(name, attrNames);
        } catch (InstanceNotFoundException e) {
            //given that the ObjectName is from queryNames
            //returned by the server this should not happen.
            //however, it is possible when nodes are not properly
            //configured.
            logAttrFailure(name,attrNames, e);
            return false;
        } catch (ReflectionException e) {
            //this should not happen either
            logAttrFailure(name,attrNames, e);
            return false;
        } 

        if (list == null) {
            //only 6.1 seems to behave this way,
            //modern weblogics throw exceptions.
            return false;
        }

        for (int i=0; i<list.size(); i++) {
            Attribute attr = (Attribute)list.get(i);
            Object obj = attr.getValue();
            if (obj != null) {
                this.attrs.put(attr.getName(), obj.toString());
            }
        }

        return true;
    }

    public String getAttribute(String name) {
        return (String)this.attrs.get(name);
    }

    public String getAttribute(String name, String defval) {
        String attr = getAttribute(name);
        if (attr == null) {
            return defval;
        }
        return attr;
    }

    public WeblogicQuery[] getChildQueries() {
        return NOOP_CHILD_QUERIES;
    }

    //should be abstract
    public String getResourceType() {
        return null;
    }

    //should be abstract
    public String getPropertyName() {
        return null;
    }

    //should be abstract
    public String getMBeanType() {
        return null;
    }

    public String getMBeanAlias() {
        return getMBeanType();
    }

    public String getMBeanNameProperty() {
        return getName();
    }

    public boolean skipParentScope() {
        return false;
    }

    public String getScope() {
        WeblogicQuery query = getParent();
        StringBuffer scope = new StringBuffer();

        scope.append("Type=");
        scope.append(getMBeanType());

        while (query != null) {
            if (!skipParentScope()) {
                scope.append(",");
                scope.append(query.getMBeanAlias());
                scope.append("=");
                scope.append(query.getMBeanNameProperty());
            }

            query = query.getParent();
        }

        return scope.toString();
    }

    public WeblogicQuery getParent() {
        return this.parent;
    }

    //cam only has two levels server -> service
    //some queries go deeper
    public WeblogicQuery getResourceParent() {
        return getParent();
    }

    public String getResourceName() {
        return
            getResourceParent().getResourceType() +
            " " + getResourceType();
    }

    //XXX this seems like it should be easier
    public String getFullName() {
        StringBuffer name = new StringBuffer();
        ArrayList names = new ArrayList();
        WeblogicQuery query = this;

        do {
            names.add(query.getQualifiedName());
        } while ((query = query.getParent()) != null);

        for (int i=names.size()-1; i>=0; i--) {
            name.append(names.get(i));
            if (i != 0) {
                name.append(" ");
            }
        }

        return name.toString();
    }

    public String getResourceFullName() {
        return
            WeblogicProductPlugin.SERVER_NAME + " " +
            getFullName() + " " +
            getResourceType();
    }

    public void setParent(WeblogicQuery value) {
        this.parent = value;
    }

    public String getQualifiedName() {
        return getName();
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getVersion() {
        return this.version;
    }

    public boolean isServer61() {
        return isServer61(this.version);
    }

    public static boolean isServer61(String version) {
        return WeblogicProductPlugin.VERSION_61.equals(version);
    }

    public boolean isServer91() {
        return isServer91(this.version);
    }
    
    public static boolean isServer91(String version) {
        return WeblogicConfig.majorVersion(version) >= 9;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        //return getAttribute(ATTR_NOTES);
        return null; //XXX
    }

    public void configure(Properties props) {
        props.setProperty(getPropertyName(), getName());
    }

    public boolean hasControl() {
        return true;
    }

    public Properties getControlConfig() {
        return NOOP_PROPERTIES;
    }

    public boolean hasResponseTime() {
        Properties config = getResponseTimeConfig();
        return config != NOOP_PROPERTIES;
    }

    public Properties getResponseTimeConfig() {
        return NOOP_PROPERTIES;
    }

    public Properties getResourceConfig() {
        Properties props = new Properties();

        configure(props);

        return props;
    }

    public Properties getCustomProperties() {
        String[] names = getCustomPropertiesNames();
        if (names.length == 0) {
            return NOOP_PROPERTIES;    
        }

        Properties cprops = new Properties();

        for (int i=0; i<names.length; i++) {
            String value = getAttribute(names[i]);
            if (value != null) {
                cprops.setProperty(names[i], value);
            }
        }

        return cprops;
    }
}
