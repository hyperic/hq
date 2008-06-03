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

package org.hyperic.hq.plugin.vim;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.util.config.ConfigResponse;

import com.vmware.vim.AboutInfo;
import com.vmware.vim.HostConfigInfo;
import com.vmware.vim.ManagedObjectReference;

public class VimHostDetector extends PlatformDetector {

    private static final Log _log =
        LogFactory.getLog(VimHostDetector.class.getName());
    private Properties _props;

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        _props = new Properties();
        String[] vimProps = {
            VimUtil.PROP_URL,
            VimUtil.PROP_USERNAME,
            VimUtil.PROP_PASSWORD,
            VimUtil.PROP_HOSTNAME
        };
        for (int i=0; i<vimProps.length; i++) {
            String val = manager.getProperty(vimProps[i]);
            if (val != null) {
                _props.setProperty(vimProps[i], val);
            }
        }
    }

    public PlatformResource getPlatformResource(ConfigResponse config)
        throws PluginException {

        config.merge(new ConfigResponse(_props), false);

        String hostname = config.getValue(VimUtil.PROP_HOSTNAME);

        PlatformResource platform =
            super.getPlatformResource(config);

        if (hostname == null) {
            _log.debug(VimUtil.PROP_HOSTNAME +
                       " not defined, skipping discovery");
            return platform;
        }

        VimUtil vim = new VimUtil();
        ConfigResponse cprops = new ConfigResponse();
        try {
            vim.init(config.toProperties());
            ManagedObjectReference mor = vim.getHost(hostname);
            HostConfigInfo info =
                (HostConfigInfo)vim.getUtil().getDynamicProperty(mor, "config");
            AboutInfo about = info.getProduct();
            platform.setDescription(about.getFullName());

            cprops.setValue("version", about.getVersion());
            cprops.setValue("build", about.getBuild());

            platform.setCustomProperties(cprops);
        } catch (Exception e) {
            _log.error(e);
        } finally {
            vim.dispose();
        }

        return platform;
    }
}
