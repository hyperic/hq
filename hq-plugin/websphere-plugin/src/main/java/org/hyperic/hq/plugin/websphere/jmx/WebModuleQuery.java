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
package org.hyperic.hq.plugin.websphere.jmx;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Properties;
import org.hyperic.hq.plugin.websphere.WebsphereProductPlugin;
import org.hyperic.hq.product.RtPlugin;
import org.hyperic.util.config.ConfigResponse;

public class WebModuleQuery extends ModuleQuery {

    public static final String MBEAN_TYPE = "WebModule";

    @Override
    public String getMBeanType() {
        return MBEAN_TYPE;
    }

    @Override
    public String getResourceType() {
        return WebsphereProductPlugin.WEBAPP_NAME;
    }

    @Override
    public String getPropertyName() {
        return WebsphereProductPlugin.PROP_WEBAPP_NAME;
    }

    @Override
    public Properties getMetricProperties() {
        Properties props = super.getMetricProperties();
        props.setProperty(WebsphereProductPlugin.PROP_WEBAPP_CONTEXT,
                File.separator + getContextName());
        return props;
    }

    /**
     * Get the name of the webapp (since the jmx mbean is named with the .war
     * extension
     *
     * @return
     */
    public String getContextName() {
        String ctxName = this.getName();
        if (ctxName.endsWith(".war")) {
            ctxName = ctxName.substring(0, ctxName.length() - 4);
        }
        return ctxName;
    }

    /**
     * Get the value of the responseTimeLogDir init-parameter as defined in this
     * modules web.xml descriptor
     *
     * @return a path, or null if the parameter is not found
     */
    private String getRtLogDir() {
        String webXml;
        try {
            webXml = (String) (this.getMBeanServer().invoke(this.getObjectName(),
                    "getDeploymentDescriptor",
                    new Object[]{},
                    new String[]{}));
            return RtPlugin.getWebAppLogDir(new ByteArrayInputStream(webXml.getBytes()));
        } catch (Exception e) {
            // e.printStackTrace();
            return null;
        }
        // return "d:\\Program Files\\WebSphere\\AppServer\\Log";
    }
}
