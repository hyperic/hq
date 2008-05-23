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

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.hyperic.hq.plugin.weblogic.WeblogicAuth;
import org.hyperic.hq.plugin.weblogic.WeblogicMetric;
import org.hyperic.hq.plugin.weblogic.WeblogicUtil;
import org.hyperic.hq.product.jmx.MBeanDumper;
import org.hyperic.hq.product.jmx.MxUtil;

public class DumpMBeans extends MBeanDumper implements PrivilegedAction {
    private MBeanServer mServer;

    public static void main(String[] args) throws Exception {
        DumpMBeans dumper = new DumpMBeans();
        dumper.getConfig(args);

        WeblogicAuth auth =
            WeblogicAuth.getInstance(dumper._config);

        auth.runAs(dumper);
    }

    protected MBeanInfo getMBeanInfo(ObjectName obj) throws Exception {
        return mServer.getMBeanInfo(obj);
    }

    protected Object getAttribute(ObjectName obj, String name) throws Exception {
        return mServer.getAttribute(obj, name);
    }

    public Object run() {
        try {
            mServer = WeblogicUtil.getMBeanServer(this._config);
            dump(mServer.queryNames(getQuery(), null));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    protected String[][] getPropertyMap() {
        return new String[][] {
            { MxUtil.PROP_JMX_URL, WeblogicMetric.PROP_ADMIN_URL },
            { MxUtil.PROP_JMX_USERNAME, WeblogicMetric.PROP_ADMIN_USERNAME },
            { MxUtil.PROP_JMX_PASSWORD, WeblogicMetric.PROP_ADMIN_PASSWORD },
        };
    }

    protected boolean isValidURL(String url) {
        return url.startsWith("t3://");
    }
}
