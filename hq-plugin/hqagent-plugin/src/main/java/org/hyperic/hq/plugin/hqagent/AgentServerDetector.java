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

package org.hyperic.hq.plugin.hqagent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.autoinventory.ServerSignature;
import org.hyperic.hq.common.shared.ProductProperties;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.sigar.Sigar;
import org.hyperic.util.config.ConfigResponse;

public class AgentServerDetector
    extends ServerDetector
    implements AutoServerDetector
{
    
    
    private static final String UNAVAILABLE = "N/A";
    private static final String AGENT_BUNDLE_HOME = AgentConfig.AGENT_BUNDLE_HOME;

    public AgentServerDetector(){
        super();
    }

    @Override
    public ServerSignature getServerSignature(){
        return new ServerSignature(AgentProductPlugin.FULL_SERVER_NAME,
                                   new String[0], new String[0], 
                                   new String[0]);
    }
    
    public List getServerResources(ConfigResponse platformConfig) throws PluginException
    {
        ArrayList res = new ArrayList();

        res.add(this.getAgentServerValue());
        return res;
    }
    
    private ServerResource getAgentServerValue(){
        ServerResource res;
        String installPath, agtName;  
        String version = ProductProperties.getVersion();
        agtName = getPlatformName() + " HQ Agent " + version;
        File dir = new File(".").getAbsoluteFile().getParentFile();
        installPath = dir.getPath();
        while (dir != null) {
            if (new File(dir, "conf/agent.properties").exists()) {
                installPath = dir.getPath();
                break;
            }
            dir = dir.getParentFile();
        }

        res = createServerResource(installPath);
        res.setName(agtName);
        res.setType(AgentProductPlugin.FULL_SERVER_NAME);
        res.setIdentifier("CAM Agent Server"); //Backwards compat
        res.setDescription("Hyperic HQ monitor Agent");

        // Set custom properties
        ConfigResponse cprop = new ConfigResponse();
        cprop.setValue("version", version);
        cprop.setValue("JavaVersion", System.getProperty("java.version"));
        cprop.setValue("JavaVendor", System.getProperty("java.vm.vendor"));
        cprop.setValue("UserHome", System.getProperty("user.home"));
        cprop.setValue("SigarVersion", Sigar.VERSION_STRING);
        cprop.setValue("SigarNativeVersion", Sigar.NATIVE_VERSION_STRING);
        cprop.setValue("AgentBundleVersion", getAgentBundleVersion());

        res.setCustomProperties(cprop);

        res.setProductConfig();
        res.setMeasurementConfig();
        return res;
    }
    
    private String getAgentBundleVersion() {
        String home = System.getProperty(AGENT_BUNDLE_HOME);
        if (home == null) {
            return UNAVAILABLE;
        }
        File bundleDir = new File(home);
        try {
            return bundleDir.getCanonicalFile().getName();
        }
        catch (IOException e) {
            return UNAVAILABLE;
        }
    }
}
