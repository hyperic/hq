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

import java.util.ArrayList;
import java.util.jar.Manifest;

import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.hq.appdef.shared.AIServerExtValue;
import org.hyperic.hq.appdef.shared.AIServiceValue;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.RuntimeResourceReport;

import org.hyperic.util.config.ConfigResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class Tomcat40RuntimeADPlugin 
    extends ServletDiscoveryPlugin
{                                                  
    // Webapp type name.  Must be overridden if subclassed.
    protected static String WEBAPP_SERVICE_TYPENAME =
        ServletProductPlugin.TOMCAT_SERVER_NAME + " " +
        ServletProductPlugin.TOMCAT_VERSION_40 + " " +
        ServletProductPlugin.WEBAPP_NAME;

    protected static Log log = LogFactory.getLog("Tomcat40RuntimeADPlugin");

    public Tomcat40RuntimeADPlugin () {}

    public RuntimeResourceReport discoverResources(int serverId,
                                                   AIPlatformValue aiplatform,
                                                   ConfigResponse config) 
        throws PluginException
    {
        RuntimeResourceReport rrr = new RuntimeResourceReport(serverId);

        Manifest mBeanInfo = getMBeanInfo(config);
        String host = getHost(config);

        // Generate a server with the same ID
        AIServerExtValue server = generateServer(serverId);

        // Do webapp discovery
        ArrayList webapps = discoverWebapps(mBeanInfo,
                                            WEBAPP_SERVICE_TYPENAME,
                                            host);
        ArrayList services = new ArrayList();
        services.addAll(webapps);

        server.setAIServiceValues((AIServiceValue[])services.
                                  toArray(new AIServiceValue[0]));
        aiplatform.addAIServerValue(server);
        rrr.addAIPlatform(aiplatform);

        return rrr;
    }
}
