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

import java.util.ArrayList;
import java.util.Properties;
import javax.management.MBeanServerConnection;

import javax.management.ObjectName;

import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigSchema;


public abstract class ServiceQuery extends JBossQuery {

    protected ObjectName objectName;
    private JBossQuery parent;
    private ServerQuery serverQuery;
    private Properties cprops;

    public abstract String getQueryName();

    void setObjectName(ObjectName objectName) {
        this.objectName = objectName;

        String name = objectName.getKeyProperty("name");
        if (name == null) {
            name = objectName.getKeyProperty(getPropertyName());
        }
        if (name != null) {
            setName(name);
        }        
    }

    public boolean apply(ObjectName name) {
        return true;
    }

    public ServiceQuery cloneInstance() {
        ServiceQuery query;

        try {
            query = (ServiceQuery)this.getClass().newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        query.setParent(getParent());
        query.setName(getName());
        
        return query;
    }
    
    public String[] getAttributeNames() {
        String type = getResourceType();
        ConfigSchema schema =
            this.getServerDetector().getCustomPropertiesSchema(type);

        return schema.getOptionNames();
    }

    @Override
    public final void getAttributes(MBeanServerConnection mServer)
        throws PluginException {

        String[] names = getAttributeNames();
        if (names.length == 0) {
            this.cprops = EMPTY_PROPERTIES;
        }
        else {
            getAttributes(mServer, getObjectName(), names);
            this.cprops = new Properties();
            for (int i=0; i<names.length; i++) {
                String key = names[i];
                String val = getAttribute(key);
                if (val != null) {
                    this.cprops.setProperty(key, val);
                }
            }
        }
    }

    public String getIdentifier() {
        return getName();
    }

    public String getResourceType() {
        return getServerQuery().getResourceType() + " " +
            getServiceResourceType();
    }

    public abstract String getServiceResourceType();

    protected abstract String getPropertyName();
    
    @Override
    public Properties getResourceConfig() {
        Properties props = new Properties();
        if (getName() != null) {
            props.setProperty(getPropertyName(), getName());
        }
        return props;
    }

    @Override
    public Properties getCustomProperties() {
        return this.cprops;
    }
    
    public ObjectName getObjectName() {
        return objectName;
    }

    public String getQualifiedName() {
        StringBuilder buf = new StringBuilder();

        ArrayList names = new ArrayList();
        String name = getName();
        if (name != null) {
            names.add(name);    
        }

        JBossQuery query = this;
        while ((query = query.getParent()) != null) {
            names.add(query.getQualifiedName());
        }

        for (int i=names.size()-1; i>=0; i--) {
            buf.append(names.get(i));
            if (i != 0) {
                buf.append(" ");
            }
        }

        if (buf.length() != 0) {
            buf.append(" ");
        }
        buf.append(getServiceResourceType());

        return buf.toString();
    }

    // tho the server query is at the root of the query tree, service
    // queries can have child service queries. therefore we store both
    // a parent ref (which could be either the root server query or an
    // intermediate service query) and an explicit ref to the root
    // server query.

    @Override
    public JBossQuery getParent() {
        return this.parent;
    }

    public void setParent(JBossQuery parent) {
        this.parent = parent;
    }

    public ServerQuery getServerQuery() {
        return this.serverQuery;
    }

    public void setServerQuery(ServerQuery serverQuery) {
        this.serverQuery = serverQuery;
    }
}
