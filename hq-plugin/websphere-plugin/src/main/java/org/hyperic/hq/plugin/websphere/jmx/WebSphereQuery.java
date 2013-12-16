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
package org.hyperic.hq.plugin.websphere.jmx;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.exception.ConnectorException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.InstanceNotFoundException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class WebSphereQuery {

    private static final Log log = LogFactory.getLog(WebSphereQuery.class.getName());
    private static final String[] NOOP_ATTRIBUTE_NAMES = new String[0];
    private static final Properties NOOP_PROPERTIES = new Properties();
    private WebSphereQuery parent;
    private String name;
    private String cell;
    private String version;
    protected AdminClient mserver;
    protected ObjectName jmxObjectName;
    protected Map attrs = new HashMap();

    public WebSphereQuery cloneInstance() {
        WebSphereQuery query;

        try {
            query = (WebSphereQuery) this.getClass().newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        query.setParent(getParent());
        query.setName(getName());
        query.setVersion(getVersion());
        query.setMBeanServer(getMBeanServer());

        return query;
    }

    public boolean apply(ObjectName name) {
        return true;
    }

    public String getResourceType() {
        return null;
    }

    public String getPropertyName() {
        return null;
    }

    public String getMBeanType() {
        return null;
    }

    public String getMBeanAlias() {
        return getMBeanType();
    }

    public String getScope() {
        WebSphereQuery query = getParent();
        StringBuffer scope = new StringBuffer();

        scope.append("type=");
        scope.append(getMBeanType());

        do {
            scope.append(",");
            scope.append(query.getMBeanAlias());
            scope.append("=");
            scope.append(query.getName());
        } while ((query = query.getParent()) != null);

        return scope.toString();
    }

    public WebSphereQuery getParent() {
        return this.parent;
    }

    public WebSphereQuery getResourceParent() {
        return getParent();
    }

    public String getResourceName() {
        return getResourceParent().getResourceType()
                + " " + getResourceType();
    }

    public String getFullName() {
        StringBuffer name = new StringBuffer();
        ArrayList names = new ArrayList();
        WebSphereQuery query = this;

        do {
            names.add(query.getName());
        } while ((query = query.getParent()) != null);

        for (int i = names.size() - 1; i >= 0; i--) {
            name.append(names.get(i));
            if (i != 0) {
                name.append(" ");
            }
        }

        return name.toString();
    }

    public void setParent(WebSphereQuery value) {
        this.parent = value;
        this.version = this.parent.version;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getCell() {
        return this.cell;
    }

    public void setCell(String value) {
        this.cell = value;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void configure(Properties props) {
        props.setProperty(getPropertyName(), getName());
    }

    public Properties getProperties() {
        Properties props = new Properties();

        configure(props);

        return props;
    }

    public Properties getMetricProperties() {
        return new Properties();
    }

    public boolean hasControl() {
        return false;
    }

    public Properties getCustomProperties() {
        if (this.attrs.size() == 0) {
            return NOOP_PROPERTIES;
        }

        Properties cprops = new Properties();
        cprops.putAll(this.attrs);
        return cprops;
    }

    public void setMBeanServer(AdminClient server) {
        this.mserver = server;
    }

    public AdminClient getMBeanServer() {
        return this.mserver;
    }

    public void setObjectName(ObjectName name) {
        this.jmxObjectName = name;
    }

    public ObjectName getObjectName() {
        return this.jmxObjectName;
    }

    public String[] getAttributeNames() {
        return NOOP_ATTRIBUTE_NAMES;
    }

    public boolean getAttributes(AdminClient mServer,
            ObjectName name) {
        return getAttributes(mServer, name, getAttributeNames());
    }

    private void logAttrFailure(ObjectName name, Exception e) {
        String msg = "Failed to get attributes for " + name;
        log.debug(msg, e);
    }

    public boolean getAttributes(AdminClient mServer,
            ObjectName name,
            String[] attrNames) {

        AttributeList list;

        if (attrNames.length == 0) {
            return true;
        }

        try {
            list = mServer.getAttributes(name, attrNames);
        } catch (InstanceNotFoundException e) {
            logAttrFailure(name, e);
            return false;
        } catch (ReflectionException e) {
            logAttrFailure(name, e);
            return false;
        } catch (ConnectorException e) {
            logAttrFailure(name, e);
            return false;
        }

        if (list == null) {
            return false;
        }

        for (int i = 0; i < list.size(); i++) {
            Attribute attr = (Attribute) list.get(i);
            Object obj = attr.getValue();
            if (obj != null) {
                this.attrs.put(attr.getName(), obj.toString());
            }
        }

        return true;
    }

    public String getAttribute(String name) {
        return (String) this.attrs.get(name);
    }

    public String getAttribute(String name, String defval) {
        String attr = getAttribute(name);
        if (attr == null) {
            return defval;
        }
        return attr;
    }
}
