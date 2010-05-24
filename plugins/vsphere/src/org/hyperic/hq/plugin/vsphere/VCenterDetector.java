package org.hyperic.hq.plugin.vsphere;
/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2008], Hyperic, Inc.
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

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import org.hyperic.hq.product.DaemonDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

public class VCenterDetector extends DaemonDetector {

    private void discoverPlatforms(ConfigResponse config)
        throws PluginException {

        Properties props = new Properties();
        props.putAll(getManager().getProperties());
        props.putAll(config.toProperties());

        VCenterPlatformDetector vpd = new VCenterPlatformDetector(props);
        try {
            vpd.discoverPlatforms();
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    protected List discoverServices(ConfigResponse config)
        throws PluginException {

        //XXX this method only gets called once a day by default
        //but we won't have the vSphere sdk config until the server
        //resource is configured.
        discoverPlatforms(config);
        return super.discoverServices(config);
    }
}
