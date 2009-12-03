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

import java.util.Properties;

import javax.management.ObjectName;

public abstract class MxServiceQuery extends MxQuery {

    private MxQuery parent;
    private MxServerQuery serverQuery;

    public abstract String getQueryName();

    public abstract String getMBeanClass();

    public abstract String getObjectNameFilter();

    public boolean apply(ObjectName name) {
        return true;
    }

    public MxServiceQuery cloneInstance() {
        MxServiceQuery query;

        try {
            query = (MxServiceQuery)this.getClass().newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        query.setParent(getParent());
        query.setName(getName());
        
        return query;
    }

    public String getIdentifier() {
        return getName();
    }

    public String getResourceType() {
        return getServerQuery().getResourceType() + " " +
            getServiceResourceType();
    }

    public abstract String getServiceResourceType();

    public Properties getResourceConfig() {
        return new Properties();
    }

    public String getQualifiedName() {
        StringBuffer buf = new StringBuffer();

        MxQuery query = this;
        while ((query = query.getParent()) != null) {
            buf.append(query.getQualifiedName());
            buf.append(" ");
        }

        String type = getServiceResourceType();
        //XXX: Revisit this, if this MBean has a name key, use that
        //rather than the full qualifed name
        String name = objectName.getKeyProperty("name");
        if (name != null) {
            //e.g. prevent "database Database"
            if (!name.equalsIgnoreCase(type)) {
                buf.append(name);
                buf.append(" ");
            }
        } else {
            buf.append(getName());
            buf.append(" ");
        }

        buf.append(type);

        return buf.toString();
    }

    // tho the server query is at the root of the query tree, service
    // queries can have child service queries. therefore we store both
    // a parent ref (which could be either the root server query or an
    // intermediate service query) and an explicit ref to the root
    // server query.

    public MxQuery getParent() {
        return this.parent;
    }

    public void setParent(MxQuery parent) {
        this.parent = parent;
    }

    public MxServerQuery getServerQuery() {
        return this.serverQuery;
    }

    public void setServerQuery(MxServerQuery serverQuery) {
        this.serverQuery = serverQuery;
    }
}
