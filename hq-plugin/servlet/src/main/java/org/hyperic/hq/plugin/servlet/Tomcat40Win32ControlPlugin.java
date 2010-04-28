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

package org.hyperic.hq.plugin.servlet;

import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.Win32ControlPlugin;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;


public class Tomcat40Win32ControlPlugin
    extends Win32ControlPlugin
{
    protected boolean isServiceRequired() {
        return false;
    }

    public ConfigSchema getConfigSchema(TypeInfo info,
                                        ConfigResponse config){
        SchemaBuilder schema = new SchemaBuilder(config);
        
        schema.add(PROP_SERVICENAME,
                   "Tomcat Windows Service name", 
                   "Apache Tomcat 4.0");
        
        return schema.getSchema();
    }

    public void doAction(String action) 
        throws PluginException
    {
        String installpath = getInstallPrefix();
        if (ServletProductPlugin.isJBossEmbeddedVersion(getTypeInfo(),
                                                        installpath)) {
            throw new PluginException("Server control not available for " +
                                      "embedded Tomcat");
        }

        super.doAction(action);
    }
}
