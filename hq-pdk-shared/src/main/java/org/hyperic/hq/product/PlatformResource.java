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

import java.util.Map;

import org.hyperic.hq.appdef.shared.AIIpValue;
import org.hyperic.hq.appdef.shared.AIPlatformValue;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

public class PlatformResource extends AIPlatformValue {

    public PlatformResource() {
        super();
        Long now = new Long(System.currentTimeMillis());
        setCTime(now);
        setMTime(now);
    }

    public void addInterface(String address, String netmask, String mac) {
        AIIpValue ip = new AIIpValue();

        ip.setAddress(address);
        if (netmask != null) {
            ip.setNetmask(netmask);
        }
        if (mac != null) {
            ip.setMACAddress(mac);
        }

        ip.setCTime(getCTime());
        ip.setMTime(getMTime());

        addAIIpValue(ip);
    }

    public void setCustomProperties(ConfigResponse config) {
        if (config == null) {
            return;
        }

        try {
            setCustomProperties(config.encode());
        } catch (EncodingException e) {
            throw new IllegalArgumentException("Error encoding config");
        }
    }
    
    public void setProductConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }

        try {
            setProductConfig(config.encode());
        } catch (EncodingException e) {
            throw new IllegalArgumentException("Error encoding config");
        }
    }

    public void setMeasurementConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }

        try {
            setMeasurementConfig(config.encode());
        } catch (EncodingException e) {
            throw new IllegalArgumentException("Error encoding config");
        }
    }

    public void setMeasurementConfig(ConfigResponse config,
                                     int logTrackLevel,
                                     boolean enableConfigTrack) {
        LogTrackPlugin.setEnabled(config,
                                  TypeInfo.TYPE_PLATFORM,
                                  logTrackLevel);
        if (enableConfigTrack) {
            ConfigTrackPlugin.setEnabled(config, TypeInfo.TYPE_PLATFORM);
        }
        setMeasurementConfig(config);
    }

    public void setControlConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }

        try {
            setControlConfig(config.encode());
        } catch (EncodingException e) {
            throw new IllegalArgumentException("Error encoding config");
        }
    }

    public void setProductConfig(Map config) {
        setProductConfig(new ConfigResponse(config));
    }

    public void setMeasurementConfig(Map config) {
        setMeasurementConfig(new ConfigResponse(config));
    }

    public void setControlConfig(Map config) {
        setControlConfig(new ConfigResponse(config));
    }

    public void setCustomProperties(Map props) {
        setCustomProperties(new ConfigResponse(props));
    }
}
