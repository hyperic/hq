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

import java.security.PrivilegedAction;
import java.util.Iterator;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.naming.Context;

import org.hyperic.hq.plugin.weblogic.WeblogicAuth;

import weblogic.jndi.Environment;
import weblogic.management.MBeanHome;
import weblogic.management.RemoteMBeanServer;

public class DumpMBeans implements PrivilegedAction {
    private String url, username, password;

    public static void main(String[] args) throws Exception {

        if (args.length > 0) {
            if (args.length < 3) {
                System.out.println("usage: url username password");
                return;
            }
        }
        else {
            args = new String[]{"t3://localhost:7001", "weblogic", "weblogic"};
        }

        DumpMBeans dumper = new DumpMBeans();
        dumper.url      = args[0];
        dumper.username = args[1];
        dumper.password = args[2];

        WeblogicAuth auth =
            WeblogicAuth.getInstance(dumper.url,
                                     dumper.username,
                                     dumper.password);
        auth.runAs(dumper);
    }
    
    public Object run() {
        try {
            dump();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void dump() throws Exception {
        Environment env = new Environment();

        env.setProviderUrl(url);
        env.setSecurityPrincipal(username);
        env.setSecurityCredentials(password);

        Context ctx = env.getInitialContext();

        MBeanHome home;

        try {
            home = (MBeanHome)ctx.lookup(MBeanHome.ADMIN_JNDI_NAME);
        } finally {
            ctx.close();
        }

        RemoteMBeanServer mServer = home.getMBeanServer();

        Iterator iter = mServer.queryMBeans(null, null).iterator();
        int maxLen = 275;

        while (iter.hasNext()) {
            ObjectInstance obj = (ObjectInstance)iter.next();
            ObjectName oName = obj.getObjectName();
            MBeanInfo info = mServer.getMBeanInfo(oName);
            
            System.out.println("MBean: " + info.getClassName());
            System.out.println("Name:  " + oName);

            MBeanAttributeInfo[] attrs = info.getAttributes();

            for (int k = 0; k < attrs.length; k++) {
                String name = attrs[k].getName();
                String value = "null";

                try {
                    Object o = mServer.getAttribute(oName, name);
                    if (o != null) {
                        value = o.toString();
                    }
                } catch (Exception e) {

                }

                if (value.length() > maxLen) {
                    value = value.substring(0, maxLen) + "...";
                }

                System.out.println("\t" + k + ". Attribute: " +
                                   name + " = " + value);
            }

            MBeanOperationInfo[] ops = info.getOperations();

            for (int i=0; i<ops.length; i++) {
                System.out.println("\t Operation: " + ops[i].getName());
            }

            System.out.println("");
        }
    }

}
