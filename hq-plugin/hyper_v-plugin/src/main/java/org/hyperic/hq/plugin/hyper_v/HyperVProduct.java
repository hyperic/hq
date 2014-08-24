/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004 - 2014], Hyperic, Inc.
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

package org.hyperic.hq.plugin.hyper_v;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.sigar.win32.RegistryKey;

public class HyperVProduct extends ProductPlugin {

    private static final Log log = LogFactory.getLog(HyperVProduct.class);
    protected static String namespace = "root\\virtualization";

    @Override
    public void init(PluginManager manager) throws PluginException {
        super.init(manager);

        try {
            RegistryKey key = RegistryKey.LocalMachine.openSubKey("SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion");
            String version = key.getStringValue("CurrentVersion").trim();
            if (version.equalsIgnoreCase("6.3")) {
                namespace = "root\\virtualization\\V2";
            }
            log.debug("[getServerResources] detected windows version '" + version + "'. Using namespace='" + namespace + "'");
        } catch (Throwable ex) {
            log.debug("[getServerResources] error detecting windows version. Using namespace='" + namespace + "'. " + ex, ex);
        }

    }

}
