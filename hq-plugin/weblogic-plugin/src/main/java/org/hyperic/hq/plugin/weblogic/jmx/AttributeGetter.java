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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * CAM invokes MeasurementPlugin.getValue once per-attribute,
 * which when using MBeanServer.getAttribute() results in
 * many roundtrips to the remote MBeanServer.  This class
 * makes one trip per-mbean and gets all the attributes
 * we will collect.
 */
public class AttributeGetter {

    public static final String PROP_ATTR_EXPIRE =
        "weblogic.attrcache.expire";
    private static Log log = LogFactory.getLog(AttributeGetter.class);

    private static Map attrCache =
        Collections.synchronizedMap(new HashMap());

    private static final int MINUTE = 60 * 1000;
    private static final int EXPIRE_DEFAULT = 5 * MINUTE;
    private long timestamp = 0;
    private int expire = EXPIRE_DEFAULT; //XXX make configurable
    private HashMap values = new HashMap();
    private String[] attrs;
    private ObjectName name;

    public static AttributeGetter getInstance(AttributeLister lister, ObjectName name) {
        AttributeGetter getter = (AttributeGetter)attrCache.get(name);

        if (getter == null) {
            String[] attrs = lister.getAttributeNames(name);

            if (attrs == null) {
                return null;
            }

            getter = new AttributeGetter();
            getter.attrs = attrs;
            getter.name = name;
            String expire = System.getProperty(PROP_ATTR_EXPIRE);
            if (expire != null) {
                getter.expire = Integer.parseInt(expire) * MINUTE;
            }
            log.debug(name + " expire=" + getter.expire / MINUTE);
            attrCache.put(name, getter);
        }

        return getter;
    }

    public Object getAttribute(MBeanServer server, String attrName)
        throws InstanceNotFoundException,
               ReflectionException,
               AttributeNotFoundException {
        
        long timeNow = System.currentTimeMillis();

        if ((timeNow - this.timestamp) > this.expire) {
            AttributeList attrList;

            if (log.isDebugEnabled()) {
                log.debug("server.getAttributes(" + this.name + ", " +
                          Arrays.asList(this.attrs) + ")");
            }

            attrList = server.getAttributes(this.name, this.attrs);

            if (attrList == null) {
                throw new AttributeNotFoundException(this.name.toString());
            }

            for (Iterator it = attrList.iterator();
                 it.hasNext();) {

                Attribute attr = (Attribute)it.next();

                this.values.put(attr.getName(), attr.getValue());
            }

            this.timestamp = timeNow;
        }

        Object value = this.values.get(attrName);

        if (value == null) {
            throw new AttributeNotFoundException(attrName);
        }

        return value;
    }
}
