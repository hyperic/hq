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

package org.hyperic.hq.plugin.weblogic.jmx;

import java.io.File;
import java.util.Properties;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;

import weblogic.management.descriptors.WebDescriptorMBean;
import weblogic.management.descriptors.webapp.FilterMBean;
import weblogic.management.descriptors.webapp.ParameterMBean;

import org.hyperic.hq.product.RtPlugin;
import org.hyperic.hq.plugin.weblogic.WeblogicMetric;
import org.hyperic.hq.plugin.weblogic.WeblogicProductPlugin;

public class WebAppComponentQuery extends ComponentQuery {
    public static final String MBEAN_TYPE = "WebAppComponentRuntime";

    private static final String[] ATTRS =
        new String[] { "URI" };
    private static final String[] RUNTIME_ATTRS =
        new String[] { "ContextRoot" };

    private String rtLogDir = null;
    private Properties rtConfig = null;
    private String webappDir = ".";

    public String getMBeanType() {
        return MBEAN_TYPE;
    }

    public String getResourceType() {
        return WeblogicProductPlugin.WEBAPP_NAME;
    }

    public String getPropertyName() {
        return WeblogicMetric.PROP_WEBAPP;
    }

    protected String getNamePrefix() {
        return this.webappPrefix;
    }

    public String[] getCustomPropertiesNames() {
        return RUNTIME_ATTRS;
    }

    public Properties getResponseTimeConfig() {
        if (this.rtLogDir == null) {
            return super.getResponseTimeConfig();
        }

        if (this.rtConfig != null) {
            return this.rtConfig;
        }

        this.rtConfig = new Properties();

        String rtLogFile = getName() + RtPlugin.LOGFILE_SUFFIX;

        this.rtConfig.setProperty(RtPlugin.CONFIG_LOGMASK, rtLogFile);
        this.rtConfig.setProperty(RtPlugin.CONFIG_LOGDIR, this.rtLogDir);
        this.rtConfig.setProperty(RtPlugin.CONFIG_INTERVAL,
                                  RtPlugin.DEFAULT_INTERVAL);

        return this.rtConfig;
    }

    public boolean getAttributes(MBeanServer mServer,
                                 ObjectName name) {
        if (!super.getAttributes(mServer, name)) {
            return false;
        }

        super.getAttributes(mServer, name, RUNTIME_ATTRS);

        if (!WeblogicProductPlugin.autoRT()) {
            return true;
        }

        String config =
            name.getDomain() + ":" +
            "Application=" + getParent().getName() + "," +
            "Name=" + getName() + "," +
            "Type=WebAppComponent";

        ObjectName configMBean;
        try {
            configMBean = new ObjectName(config);
        } catch (MalformedObjectNameException e) {
            //notgonnahappen
            WeblogicDiscover.getLog().error(e.getMessage(), e);
            return true;
        }

        //web.xml is already parsed into MBeans,
        //just need to dig out the rtlog dir.

        try {
            WebDescriptorMBean descriptor = (WebDescriptorMBean)
                mServer.invoke(configMBean,
                               "findOrCreateWebDescriptor",
                               new Object[0], new String[0]);

            FilterMBean[] filters =
                descriptor.getWebAppDescriptor().getFilters();

            for (int i=0; i<filters.length; i++) {
                ParameterMBean[] params = filters[i].getInitParams();

                for (int j=0; j<params.length; j++) {
                    String pName = params[j].getParamName();

                    if (pName.equals(RtPlugin.PARAM_LOG_DIR)) {
                        this.rtLogDir = params[j].getParamValue();
                        break;
                    }
                }
            }
        } catch (Throwable e) {
            //yes, we are catching Throwable.
            WeblogicDiscover.getLog().debug("RT config lookup failed: " + e);
            return true;
        }

        if (WeblogicDiscover.getLog().isDebugEnabled()) {
            WeblogicDiscover.getLog().debug(getName() + " RT config=" +
                                            getResponseTimeConfig());
        }

        String path = getParent().getAttribute("Path");
        if (path != null) {
            super.getAttributes(mServer, configMBean, ATTRS);
            String uri = getAttribute(ATTRS[0]);
            if (uri != null) {
                this.webappDir = path + File.separator + uri;
            }
        }

        return true;
    }

    public Properties getResourceConfig() {
        Properties props = super.getResourceConfig();
        props.setProperty(WeblogicMetric.PROP_WEBAPP_DIR, this.webappDir);
        return props;
    }
}
