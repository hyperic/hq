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

import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.config.ConfigResponse;

public class TomcatServerControlPlugin 
    extends ServletEngineControlPlugin
{
    public TomcatServerControlPlugin() {
        super();
        type = ServletProductPlugin.TOMCAT_NAME;
    }

    protected void updateOptions(TypeInfo info) {
        StringConfigOption startupScript =
            new StringConfigOption(ServletEngineControlPlugin.
                                   PROP_STARTUP_SCRIPT,
                                   "Startup script. If relative, the " +
                                   "basedir will be prepended. It will " +
                                   "be called with start/stop " +
                                   "parameters appended at the end",
                                   "bin/catalina.sh");
        configOptions.add(startupScript);
    }        
    
    protected String[] getCommandEnv() {
        if (baseDir != null) {
            getLog().debug("Setting catalina_base to " + baseDir);
            getLog().debug("Setting catalina_home to " + getInstallPrefix());
            return new String [] {
                "CATALINA_BASE=" + baseDir, 
                "CATALINA_HOME=" + getInstallPrefix()
            };
        }
        else {
            getLog().debug("Cannot determine base directory");
            return null;
        }
    }    
    
    protected void initScript(ConfigResponse config ) 
        throws PluginException
    {
        baseDir = config.getValue(ServletProductPlugin.PROP_TOMCAT_BASE);
        if("".equals(baseDir)) 
            baseDir=null;
        
        String startupScript = 
            config.getValue(ServletEngineControlPlugin.PROP_STARTUP_SCRIPT);
        setControlProgram(startupScript);        
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
