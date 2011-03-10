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

import java.util.Properties;

import javax.management.MBeanServerConnection;
import javax.naming.Context;

import org.hyperic.hq.product.jmx.MBeanDumper;
import org.hyperic.hq.product.jmx.MxUtil;

public class JBossMBeanDumper extends MBeanDumper {

    protected boolean isValidURL(String url) {
        return url.startsWith("jnp://") || super.isValidURL(url);
    }

    protected String[][] getPropertyMap() {
        return new String[][] {
            { MxUtil.PROP_JMX_URL, Context.PROVIDER_URL },
            { MxUtil.PROP_JMX_USERNAME, Context.SECURITY_PRINCIPAL },
            { MxUtil.PROP_JMX_PASSWORD, Context.SECURITY_CREDENTIALS },
        };
    }

    protected MBeanServerConnection getMBeanServerConnection(Properties config)
        throws Exception {

        return (MBeanServerConnection)JBossUtil.getMBeanServerConnection(_config);
    }

    //java -jar pdk/lib/hq-pdk.jar jboss JBossMBeanDumper
    public static void main(String[] args) throws Exception {
        new JBossMBeanDumper().dump(args);
    }
}
