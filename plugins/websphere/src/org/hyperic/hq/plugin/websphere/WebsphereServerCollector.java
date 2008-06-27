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

package org.hyperic.hq.plugin.websphere;

import javax.management.j2ee.statistics.Stats;

import org.hyperic.hq.product.PluginException;

import com.ibm.websphere.management.AdminClient;

public class WebsphereServerCollector extends WebsphereCollector {

    private boolean isJVM;
    private String[][] attrs;

    //MBean Attribute -> legacy pmi name
    private static final String[][] TX_ATTRS = {
        { "GlobalBegunCount", "globalTransBegun" },
        { "GlobalInvolvedCount", "globalTransInvolved" },
        { "LocalBegunCount", "localTransBegun" },
        { "ActiveCount", "activeGlobalTrans" },
        { "LocalActiveCount", "activeLocalTrans" },
        { "OptimizationCount", "numOptimization" },
        { "CommittedCount", "globalTransCommitted" },
        { "LocalCommittedCount", "localTransCommitted" },
        { "RolledbackCount", "globalTransRolledBack" },
        { "LocalRolledbackCount", "localTransRolledBack" },
        { "GlobalTimeoutCount", "globalTransTimeout" },
        { "LocalTimeoutCount","localTransTimeout" }
    };

    protected void init(AdminClient mServer) throws PluginException {
        super.init(mServer);

        String module = getProperties().getProperty("Module");

        if (module.equals("jvmRuntimeModule")) {
            isJVM = true;
            this.name =
                newObjectNamePattern("name=JVM," +
                                     "type=JVM," +
                                     "j2eeType=JVM," +
                                     getServerAttributes());

            this.name = resolve(mServer, this.name);
        }
        else if (module.equals("transactionModule")) {
            this.name =
                newObjectNamePattern("type=TransactionService," +
                                     "j2eeType=JTAResource");

            this.name = resolve(mServer, this.name);
            this.attrs = TX_ATTRS;
        }
        else if (module.equals("servletSessionsModule")) {
            this.name = null; //XXX
            setSource(module);
        }
        else if (module.equals("webappModule")) {
            this.name = null; //XXX
            setSource(module);
        }
        else if (module.equals("beanModule")) {
            this.name = null; //XXX
            setSource(module);
        }
        else if (module.equals("threadPoolModule")) {
            this.name = null; //XXX
            setSource(module);
        }
        else if (module.equals("connectionPoolModule")) {
            this.name = null; //XXX
            setSource(module);
        }
    }

    public void collect() {
        if (this.name == null) {
            return; //XXX see above
        }
        AdminClient mServer = getMBeanServer();
        if (mServer == null) {
            return;
        }

        setAvailability(true);

        Stats stats =
            (Stats)getStats(mServer, this.name);

        if (stats == null) {
            return;
        }

        if (isJVM) {
            double total = getStatCount(stats, "HeapSize");
            double used  = getStatCount(stats, "UsedMemory");
            setValue("totalMemory", total);
            setValue("usedMemory", used);
            setValue("freeMemory", total-used);
        }
        else {
            collectStatCount(stats, this.attrs);
        }
    }
}
