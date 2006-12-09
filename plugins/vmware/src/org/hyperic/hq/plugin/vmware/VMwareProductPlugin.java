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

package org.hyperic.hq.plugin.vmware;

import java.io.File;
import java.io.IOException;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.sigar.vmware.VMControlLibrary;

public class VMwareProductPlugin extends ProductPlugin {

    private static boolean isLoaded = false;

    static boolean isLoaded() {
        return isLoaded;
    }

    static void checkIsLoaded() throws PluginException {
        if (!isLoaded()) {
            throw new PluginException("VMware control library not available");
        }
    }

    public void init(PluginManager manager)
        throws PluginException {

        super.init(manager);

        File dir = getWorkDir("lib");

        if (dir != null) {
            try {
                VMControlLibrary.link(dir.getPath());
            } catch (IOException e) {
                getLog().error(e.getMessage());
            }
            isLoaded = VMControlLibrary.isLoaded();
            if (isLoaded) {
                getLog().info("Using vmcontrol library=" +
                              VMControlLibrary.getSharedLibrary());
            }
            else {
                getLog().debug("vmcontrol library not available");
            }
        }
    }
}
