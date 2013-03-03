/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2013], Hyperic, Inc.
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

package org.hyperic.hq.autoinventory.scanimpl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.appdef.shared.AIServerValue;
import org.hyperic.hq.autoinventory.AutoinventoryException;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.PluginLoader;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;

import java.util.List;

public class NullScan extends ScanMethodBase {
    private static final Log log = LogFactory.getLog(NullScan.class);
    
    public NullScan(){
        this._authorityLevel = 10;
    }

    public String getName(){
        return "NullScan"; 
    }

    public String getDisplayName(){
        return "Null Scan"; 
    }

    public String getDescription(){
        return "Scan with prepopulated data"; 
    }

    private static final ConfigOption[] OPTS = new ConfigOption[0];

    protected ConfigOption[] getOptionsArray(){
        return OPTS;
    }

    public void scan(ConfigResponse platformConfig, ServerDetector[] serverDetectors) {

        for (ServerDetector detector : serverDetectors) {

            if (!(detector instanceof AutoServerDetector)) {
                continue;
            }

            if (log.isDebugEnabled()) {
                log.debug("Running AutoServerDetector for: " + detector.getTypeInfo().getName());
            }

            PluginLoader.setClassLoader(detector);
            try {
                List servers = ((AutoServerDetector) detector).getServerResources(platformConfig);
                if (servers != null) {
                    for (Object server : servers) {
                        AIServerValue xsrv;
                        if (server instanceof AIServerValue) {
                            xsrv = (AIServerValue) server;
                        } else {
                            xsrv = (AIServerValue) ((ServerResource) server).getResource();
                        }

                        if(_autoApproveConfig!=null) { // don't needed
                            if (_autoApproveConfig.isAutoApproved(xsrv.getServerTypeName())) {
                                xsrv.setAutoApprove(true);
                            }
                        }
                    }

                    _state.addServers(this, servers);
                    _state.setAreServersIncluded(true);
                }
            } catch (Throwable e) {
                log.error("AutoScan failed for " + detector.getTypeInfo().getName(), e);
           } finally {
                PluginLoader.resetClassLoader(detector);
            }
        }
    }
}
