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

import org.hyperic.hq.product.GenericPlugin;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.TypeBuilder;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;

public class AgentProductPlugin 
    extends ProductPlugin
{
    public static final String NAME = "camagent";

    static final String SERVER_NAME = "HQ Agent";
    static final String SERVER_DESC = "Server";

    public static final String FULL_SERVER_NAME  = SERVER_NAME;

    public AgentProductPlugin() {
        this.setName(NAME);
    }

    public ConfigSchema getCustomPropertiesSchema(String name) {
        ConfigSchema schema = new ConfigSchema();

        StringConfigOption opt =
            new StringConfigOption("version",
                                   "Agent Version");
        schema.addOption(opt);

		opt = new StringConfigOption("build", 
									"Agent Build");
		opt.setOptional(true);
		schema.addOption(opt);

        opt = new StringConfigOption("JavaVersion",
                                     "Java Version");
        schema.addOption(opt);

        opt = new StringConfigOption("JavaVendor",
                                     "Java Vendor");
        schema.addOption(opt);

        opt = new StringConfigOption("UserHome",
                                     "User Home");
        schema.addOption(opt);

        opt = new StringConfigOption("SigarVersion",
                                     "Sigar Version");
        schema.addOption(opt);

        opt = new StringConfigOption("SigarNativeVersion",
                                     "Sigar Native Version");
        schema.addOption(opt);
        
        opt = new StringConfigOption("AgentBundleVersion",
        "Agent Bundle Version");
        schema.addOption(opt);        

        return schema;
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config){
        return new ConfigSchema();
    }

    public GenericPlugin getPlugin(String type, TypeInfo entity)
    {
        if(type.equals(ProductPlugin.TYPE_MEASUREMENT)){
            return new AgentMeasurementPlugin();
        } else if(type.equals(ProductPlugin.TYPE_AUTOINVENTORY)){
            if(entity.getType() == TypeInfo.TYPE_SERVER){
                return new AgentServerDetector();
            }
        } else if(type.equals(ProductPlugin.TYPE_LOG_TRACK)){
            if(entity.getType() == TypeInfo.TYPE_SERVER){
                return new AgentLogTrackPlugin();
            }
        } else if(type.equals(ProductPlugin.TYPE_CONFIG_TRACK)) {
            if(entity.getType() == TypeInfo.TYPE_SERVER){
                return new AgentConfigTrackPlugin();
            }
        }
        
        return null;
    }

    public TypeInfo[] getTypes() {
        TypeBuilder types = new TypeBuilder(SERVER_NAME, SERVER_DESC);

        types.addServer(TypeBuilder.NO_VERSION);
        return types.getTypes();
    }
}
