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

package org.hyperic.hq.plugin.appha;

import org.hyperic.hq.hqapi1.types.Ip;
import org.hyperic.hq.hqapi1.types.Resource;
import org.hyperic.hq.hqapi1.types.ResourceConfig;
import org.hyperic.hq.hqapi1.types.ResourceInfo;
import org.hyperic.hq.hqapi1.types.ResourceProperty;
import org.hyperic.util.config.ConfigResponse;

//Resource object helpers
public class VSphereResource extends Resource {

    public void addInfo(String key, String val) {
        ResourceInfo info = new ResourceInfo();
        info.setKey(key);
        info.setValue(val);
        getResourceInfo().add(info);
    }

    public void addProperty(String key, String val) {
        ResourceProperty prop = new ResourceProperty();
        prop.setKey(key);
        prop.setValue(val);
        getResourceProperty().add(prop);
    }

    public void addConfig(String key, String val) {
        ResourceConfig prop = new ResourceConfig();
        prop.setKey(key);
        prop.setValue(val);
        getResourceConfig().add(prop);
    }

    public void addConfig(ConfigResponse config) {
        for (Object k : config.getKeys()) {
            String key = (String)k;
            addConfig(key, config.getValue(key));
        }
    }

    public void addProperties(ConfigResponse config) {
        for (Object k : config.getKeys()) {
            String key = (String)k;
            addProperty(key, config.getValue(key));
        }
    }

    public void addIp(String address, String netmask, String mac) {
        Ip ip = new Ip();
        ip.setAddress(address);
        ip.setMac(mac);
        ip.setNetmask(netmask);
        getIp().add(ip);
    }

    public void setFqdn(String fqdn) {
        addInfo("fqdn", fqdn);
    }
}
