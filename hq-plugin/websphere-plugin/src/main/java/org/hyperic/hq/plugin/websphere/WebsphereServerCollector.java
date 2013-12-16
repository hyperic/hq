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

import com.ibm.websphere.management.AdminClient;
import javax.management.ObjectName;
import javax.management.j2ee.statistics.Stats;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;

public class WebsphereServerCollector extends WebsphereCollector {

    private static final Log log =
            LogFactory.getLog(WebsphereServerCollector.class.getName());
    private boolean isJVM;
    //MBean Attribute -> legacy pmi name
    private static final String[][] TX_ATTRS = {
        {"GlobalBegunCount", "globalTransBegun"},
        {"GlobalInvolvedCount", "globalTransInvolved"},
        {"LocalBegunCount", "localTransBegun"},
        {"ActiveCount", "activeGlobalTrans"},
        {"LocalActiveCount", "activeLocalTrans"},
        {"OptimizationCount", "numOptimization"},
        {"CommittedCount", "globalTransCommitted"},
        {"LocalCommittedCount", "localTransCommitted"},
        {"RolledbackCount", "globalTransRolledBack"},
        {"LocalRolledbackCount", "localTransRolledBack"},
        {"GlobalTimeoutCount", "globalTransTimeout"},
        {"LocalTimeoutCount", "localTransTimeout"},
        {"GlobalTranTime"},
        {"LocalTranTime"},
        {"GlobalBeforeCompletionTime"},
        {"GlobalPrepareTime"},
        {"GlobalCommitTime"},
        {"LocalCommitTime"},
        {"LocalRolledbackCount"},
    };

    protected void init(AdminClient mServer) throws PluginException {
        String serverName = getProperties().getProperty("server.name");
        String module = getProperties().getProperty("Module");
        log.info("[init] [" + serverName + "] module=" + module);

        String name;
        if (module.equals("jvmRuntimeModule")) {
            isJVM = true;
            name = "name=JVM,type=JVM,j2eeType=JVM";
        } else if (module.equals("transactionModule")) {
            name = "type=TransactionService,j2eeType=JTAResource";
        } else if (module.equals("adminModule")) {
            name = "name=JVM,type=JVM";
        } else {
            throw new PluginException("Unexpected module '" + module + "'");
        }

        ObjectName on = newObjectNamePattern(name + "," + getServerAttributes());
        on = resolve(mServer, on);
        log.debug("[init] [" + serverName + "] name=" + on);
        setObjectName(on);

        // check server properties.
        getStats(mServer, getObjectName());
    }

    public void collect(AdminClient mServer) throws PluginException {
        log.info("[collect] "+getProperties());
        if (getModuleName().equalsIgnoreCase("adminModule")) {
            setValue("NumJVMs", count(mServer));
        } else {
            Stats stats = (Stats) getStats(mServer, getObjectName());

            if (stats != null) {
                if (isJVM) {
                    setValue("totalMemory", getStatCount(stats, "HeapSize"));
                    setValue("usedMemory", getStatCount(stats, "UsedMemory"));
                    setValue("freeMemory", getStatCount(stats, "freeMemory"));
                    setValue("ProcessCpuUsage", getStatCount(stats, "ProcessCpuUsage"));
                } else {
                    collectStatCount(stats, TX_ATTRS);
                }
            } else {
                log.debug("no Stats");
            }
        }
    }

    private double count(AdminClient mServer) throws PluginException {
        try {
            return WebsphereUtil.getMBeanCount(mServer, getObjectName(), null);
        } catch (Exception ex) {
            throw new PluginException(ex.getMessage(), ex);
        }
    }
}
