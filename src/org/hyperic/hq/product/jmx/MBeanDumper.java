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

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.remote.JMXServiceURL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MBeanDumper {

    private static Log log = LogFactory.getLog(MBeanDumper.class);

    private static final String DEFAULT_URL =
        "service:jmx:rmi://localhost/jndi/rmi://localhost:1099/jmxrmi";

    private static boolean isValidObjectName(String name) {
        try {
            new ObjectName(name);
            return true;
        } catch (MalformedObjectNameException e) {
            return false;
        }
    }

    private static boolean isValidURL(String url) {
        if (url.startsWith(MxUtil.PTQL_PREFIX)) {
            return true;
        }
        try {
            new JMXServiceURL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static void main(String[] args) throws Exception {
        String url = DEFAULT_URL;
        String query = "*:*";

        for (int i=0; i<args.length; i++) {
            String arg = args[i];
            if (isValidURL(arg)) {
                url = arg;
            }
            else if (isValidObjectName(arg)) {
                query = arg;
            }
            else {
                System.err.println("Invalid argument: " + arg);
                return;
            }
        }

        Properties config = new Properties();
        config.putAll(System.getProperties());

        if (config.getProperty(MxUtil.PROP_JMX_URL) == null) {
            config.setProperty(MxUtil.PROP_JMX_URL, url);
        }

        //compat
        String user = System.getProperty("user");
        String pass = System.getProperty("pass");
        if (user != null) {
            config.setProperty(MxUtil.PROP_JMX_USERNAME, user);
        }
        if (pass != null) {
            config.setProperty(MxUtil.PROP_JMX_PASSWORD, pass);
        }

        MBeanServerConnection mServer = MxUtil.getMBeanServer(config);
        ObjectName qName = new ObjectName(query);

        Iterator iter = mServer.queryNames(qName, null).iterator();

        while (iter.hasNext()) {
            ObjectName obj = (ObjectName)iter.next();
            try {
                MBeanInfo info = mServer.getMBeanInfo(obj);
            
                System.out.println("MBean: " + info.getClassName());
                System.out.println("Name:  " + obj);

                MBeanAttributeInfo[] attrs = info.getAttributes();
                for (int k = 0; k < attrs.length; k++) {
                    String name = attrs[k].getName();
                    String value = "null";

                    try {
                        Object o = mServer.getAttribute(obj, name);
                        if (o != null) {
                            if (o.getClass().isArray()) {
                                value = Arrays.asList((Object[])o).toString();
                            }
                            else {
                                value = o.toString();
                            }
                        }
                    } catch (Exception e) {
                        value = "ERROR";
                        if (log.isDebugEnabled()) {
                            e.printStackTrace();
                        }
                    }
                    String perms = "";
                    if (attrs[k].isReadable()) {
                        perms += "r";
                    }
                    if (attrs[k].isWritable()) {
                        perms += "w";
                    }
                    System.out.println("\t" + k + ". Attribute: " +
                                       name + " = " + value +
                                       " (" + perms + ")");
                }

                MBeanOperationInfo[] ops = info.getOperations();

                for (int i=0; i<ops.length; i++) {
                    ArrayList sig = new ArrayList();
                    MBeanParameterInfo[] params = ops[i].getSignature();
                    for (int j=0; j<params.length; j++) {
                        sig.add(params[j].getType());
                    }
                    System.out.println("\t Operation: " +
                                       ops[i].getReturnType() + " " +
                                       ops[i].getName() + " " + sig);
                }
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    e.printStackTrace();
                }
            }

            System.out.println("");
        }
    }
}
