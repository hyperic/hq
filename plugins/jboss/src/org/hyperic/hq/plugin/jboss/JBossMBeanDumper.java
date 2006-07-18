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

package org.hyperic.hq.plugin.jboss;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import javax.management.*;
import javax.naming.Context;

import org.hyperic.hq.plugin.jboss.JBossUtil;
import org.jboss.jmx.adaptor.rmi.RMIAdaptor;

public class JBossMBeanDumper {

    //java -jar pdk/lib/hq-product.jar jboss JBossMBeanDumper
    public static void main(String[] args) throws Exception {
        RMIAdaptor mServer = null;
        String url;

        if (args.length == 1) {
            url = args[0];
        }
        else {
            url = "jnp://localhost:1099";
        }

        Properties config = new Properties();
        config.setProperty(Context.PROVIDER_URL, url);
        mServer = JBossUtil.getMBeanServer(config);

        Iterator iter = mServer.queryNames(null, null).iterator();

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
                            value = o.toString();
                        }
                    } catch (Exception e) {
                        
                    }

                    System.out.println("\t" + k + ". Attribute: " +
                                       name + " = " + value);
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

            }

            System.out.println("");
        }
    }
}
