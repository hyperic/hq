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

import java.util.Iterator;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.ProductPluginManager;

class GenericServiceQuery extends ServiceQuery {

    private String type;
    private Map props;
    private String propName = "name"; //ConfigSchema name

    GenericServiceQuery() { }

    void setType(String type) {
        this.type = type;        
    }

    String getMBeanClass() {
        return getProperty("MBEAN_CLASS");
    }
    
    public ServiceQuery cloneInstance() {
        GenericServiceQuery query =
            (GenericServiceQuery)super.cloneInstance();
        
        query.type = this.type;
        query.props = this.props;
        query.propName = this.propName;

        return query;
    }

    protected String getProperty(String name) {
        return getServerDetector().getTypeProperty(this.type, name);
    }

    private StringBuffer appendComma(StringBuffer buf) {
        char c = buf.charAt(buf.length()-1); 
        if ((c != ',') && (c != ':')) {
            buf.append(',');
        }
        return buf;
    }

    private ObjectName getObjectNameProperty() {
        String name = getProperty(PROP_OBJECT_NAME);
        if (name == null) {
            String msg =
                this.type +
                " service did not define property " +
                PROP_OBJECT_NAME;
            throw new IllegalArgumentException(msg);
        }
        try {
            return new ObjectName(name);
        } catch (MalformedObjectNameException e) {
            String msg =
                this.type +
                " service defined malformed " +
                PROP_OBJECT_NAME + "=" + name;
            throw new IllegalArgumentException(msg);
        }        
    }

    public String getQueryName() {
        ObjectName name = getObjectNameProperty();

        StringBuffer buf = new StringBuffer();
        
        buf.append(name.getDomain()).append(":");

        boolean isPattern = false;
        this.props = name.getKeyPropertyList();
        for (Iterator it=props.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();
            String key = (String)entry.getKey();
            String val = (String)entry.getValue();
            if (val.startsWith("%") && val.endsWith("%")) {
                this.props.put(key, "*");
                this.propName = key;
                isPattern = true;
                continue;
            }
            buf.append(key).append('=').append(val);
            if (it.hasNext()) {
                appendComma(buf);
            }
        }
        if (isPattern) {
            appendComma(buf).append('*');
        }

        return buf.toString();
    }

    //can't use queryNames w/ wildcard for property values like so:
    //jboss.ejb:service=EJB,jndiName=*
    //can't use QueryExp either because that is applied to attributes
    //of the MBean, not the attributes of the ObjectName itself
    public boolean apply(ObjectName name) {
        Map props = name.getKeyPropertyList();
        if (this.props.size() != props.size()) {
            return false;
        }
        for (Iterator it=this.props.entrySet().iterator();
             it.hasNext();)
        {
            Map.Entry entry = (Map.Entry)it.next();
            String key = (String)entry.getKey();
            String val = (String)entry.getValue();
            String compare = (String)props.get(key);
            if (compare == null) {
                return false;
            }
            if (val.equals("*")) {
                continue;
            }
            if (!val.equals(compare)) {
                return false;
            }
        }
        return true;
    }

    public String getServiceResourceType() {
        //the name="..." attribute value from <service> tag in the plugin .xml
        return getServerDetector().getTypeNameProperty(this.type);
    }

    protected String getPropertyName() {
        return this.propName;
    }

    public boolean hasControl() {
        //XXX this functionality should be elsewhere
        ProductPluginManager ppm =
            (ProductPluginManager)getServerDetector().getManager().getParent();
        GenericPlugin plugin = ppm.getControlPlugin(this.type);
        return plugin != null;
    }
}
