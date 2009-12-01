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

package org.hyperic.hq.plugin.xen;

import java.util.Map;
import java.util.Properties;

import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.util.config.ConfigResponse;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class XenHostDetector extends PlatformDetector {

    private static final Log _log =
        LogFactory.getLog(XenHostDetector.class.getName());
    private Properties _props;

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        _props = new Properties();
        String[] xenProps = {
            XenUtil.PROP_URL,
            XenUtil.PROP_USERNAME,
            XenUtil.PROP_PASSWORD,
            XenUtil.PROP_PLATFORM_UUID
        };
        for (int i=0; i<xenProps.length; i++) {
            String val = manager.getProperty(xenProps[i]);
            if (val != null) {
                _props.setProperty(xenProps[i], val);
            }
        }
    }

    private void setValue(ConfigResponse config, String key, String val) {
        if (val == null) {
            return;
        }
        config.setValue(key, val);
    }

    public PlatformResource getPlatformResource(ConfigResponse config)
        throws PluginException {

        config.merge(new ConfigResponse(_props), false);

        String uuid = config.getValue(XenUtil.PROP_PLATFORM_UUID);

        PlatformResource platform =
            super.getPlatformResource(config);

        if (uuid == null) {
            return platform;
        }

        Properties props = config.toProperties();
        Connection conn = XenUtil.connect(props);

        Host host = XenUtil.getHost(conn, props);
        ConfigResponse cprops = new ConfigResponse();
        try {
            _log.debug("Connected to: " + host.getHostname(conn));
            platform.setDescription(host.getNameDescription(conn));

            Map<String,String> version = host.getSoftwareVersion(conn); 
            setValue(cprops, "version", version.get("product_version"));
            setValue(cprops, "brand", version.get("product_brand"));
            setValue(cprops, "build_id", version.get("hg_id"));
            setValue(cprops, "hostname", version.get("hostname"));
            setValue(cprops, "date", version.get("date"));
            setValue(cprops, "build_number", version.get("build_number"));
            setValue(cprops, "linux", version.get("linux"));

            platform.setCustomProperties(cprops);
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }

        return platform;
    }
}
