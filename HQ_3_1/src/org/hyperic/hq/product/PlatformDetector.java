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

package org.hyperic.hq.product;

import org.hyperic.sigar.OperatingSystem;
import org.hyperic.util.config.ConfigResponse;

/**
 * Detect platform attributes
 */
public class PlatformDetector extends GenericPlugin {

    public static final boolean IS_WIN32 =
        OperatingSystem.IS_WIN32;

    public static final String[] UNIX_PLATFORM_NAMES =
        OperatingSystem.UNIX_NAMES;
    
    public static String[] WIN32_PLATFORM_NAMES =
        OperatingSystem.WIN32_NAMES;
    
    public static final String[] PLATFORM_NAMES =
        OperatingSystem.NAMES;

    public static boolean isSupportedPlatform(String name) {
        return OperatingSystem.isSupported(name);
    }

    public static boolean isWin32(String name) {
        return OperatingSystem.isWin32(name);
    }

    private String getValue(ConfigResponse config, String key)
        throws PluginException {
        String value = config.getValue(key);
        if (value == null) {
            throw new PluginException("Cannot detect platform with " + key + "=null");
        }
        return value;
    }
    
    public PlatformResource getPlatformResource(ConfigResponse config) 
        throws PluginException {

        if (config == null) {
            throw new PluginException("Cannot detect platform with config=null");
        }
        
        String type = getValue(config, ProductPlugin.PROP_PLATFORM_TYPE);
        String fqdn = getValue(config, ProductPlugin.PROP_PLATFORM_FQDN);
        String addr = getValue(config, ProductPlugin.PROP_PLATFORM_IP);
        String name = config.getValue(ProductPlugin.PROP_PLATFORM_NAME);

        PlatformResource platform = new PlatformResource();
        platform.setFqdn(fqdn);
        platform.setPlatformTypeName(type);
        platform.setName(name);
        //we license x number of platforms, this counts as 1
        platform.setCpuCount(new Integer(1));
        platform.addInterface(addr, null, null);

        return platform;
    }
}
