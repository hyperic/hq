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
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.exception.ConnectorException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Properties;
import javax.management.*;
import org.hyperic.hq.plugin.websphere.WebsphereProductPlugin;

public class DumpMBeans {

    public static void main(String[] args) throws Exception {
        boolean statsOnly = false;
        String host =
                System.getProperty(WebsphereProductPlugin.PROP_ADMIN_HOST,
                "localhost");
        String port =
                System.getProperty(WebsphereProductPlugin.PROP_ADMIN_PORT,
                "8880");
        String user =
                System.getProperty(WebsphereProductPlugin.PROP_USERNAME);
        String pass =
                System.getProperty(WebsphereProductPlugin.PROP_PASSWORD);

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-stats")) {
                statsOnly = true;
            }
        }

        Properties props = new Properties();
        props.setProperty(AdminClient.CONNECTOR_TYPE,
                AdminClient.CONNECTOR_TYPE_SOAP);

        props.setProperty(AdminClient.CONNECTOR_HOST, host);
        props.setProperty(AdminClient.CONNECTOR_PORT, port);

        if ((user != null) && (pass != null)) {
            props.setProperty(AdminClient.USERNAME, user);
            props.setProperty(AdminClient.PASSWORD, pass);
            props.setProperty(AdminClient.CONNECTOR_SECURITY_ENABLED, "true");
        }
        AdminClient mServer = null;

        try {
            mServer = AdminClientFactory.createAdminClient(props);
        } catch (ConnectorException e) {
            System.out.println("Exception creating admin client: " + e);
            e.printStackTrace();
        }

        Iterator iter = mServer.queryNames(null, null).iterator();

        while (iter.hasNext()) {
            ObjectName obj = (ObjectName) iter.next();
            MBeanInfo info = mServer.getMBeanInfo(obj);

            System.out.println("");
            System.out.println("MBean: " + info.getClassName());
            System.out.println("Name:  " + obj);

            if (statsOnly) {
                try {
                    Object stats =
                            mServer.getAttribute(obj, "stats");
                    System.out.println("Stats-Class: "
                            + stats.getClass().getName());
                    System.out.println("Stats: " + stats);
                } catch (AttributeNotFoundException e) {
                    System.out.println("Stats: NONE");
                } catch (Exception e) {
                    System.out.println("Stats: " + e);
                }
                continue;
            }

            MBeanAttributeInfo[] attrs = info.getAttributes();
            for (int k = 0; k < attrs.length; k++) {

                String name = attrs[k].getName();
                String value = "null";

                try {
                    Object o = mServer.getAttribute(obj, name);
                    if (o != null) {
                        if (o.getClass().isArray()) {
                            value = Arrays.asList((Object[]) o).toString();
                        } else {
                            value = o.toString();
                        }
                    }
                } catch (Exception e) {
                    value = e.getMessage();
                }

                System.out.println("\t" + k + ". Attribute: "
                        + name + " = " + value);

            }

            MBeanOperationInfo[] ops = info.getOperations();

            for (int i = 0; i < ops.length; i++) {
                ArrayList sig = new ArrayList();
                MBeanParameterInfo[] params = ops[i].getSignature();
                for (int j = 0; j < params.length; j++) {
                    sig.add(params[j].getType());
                }
                System.out.println("\t Operation: "
                        + ops[i].getReturnType() + " "
                        + ops[i].getName() + " " + sig);
            }
        }
    }
}
