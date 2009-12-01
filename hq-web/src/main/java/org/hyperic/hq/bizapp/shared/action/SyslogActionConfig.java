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

package org.hyperic.hq.bizapp.shared.action;

import org.hyperic.hq.events.ActionConfigInterface;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.InvalidOptionException;
import org.hyperic.util.config.InvalidOptionValueException;
import org.hyperic.util.config.StringConfigOption;

public class SyslogActionConfig implements ActionConfigInterface {
    
    public final static String CFG_META = "meta";
    public final static String CFG_PROD = "product";
    public final static String CFG_VER  = "version";
    
    private static String _implementor =
        "org.hyperic.hq.bizapp.server.action.log.SyslogAction";
    
    private String _meta;
    private String _product;
    private String _version;
    
    public SyslogActionConfig(){
    }
    
    public SyslogActionConfig(String meta, String product, String version) {
        _meta    = meta;
        _product = product;
        _version = version;
    }

    public void init(ConfigResponse config) {
        setMeta(config.getValue(CFG_META));
        setProduct(config.getValue(CFG_PROD));
        setVersion(config.getValue(CFG_VER));
    }
    
    public ConfigSchema getConfigSchema() {
        StringConfigOption action;
        ConfigSchema res = new ConfigSchema();

        action = new StringConfigOption(CFG_META, "Meta Project", "");
        action.setMinLength(0);
        res.addOption(action);
        
        action = new StringConfigOption(CFG_PROD, "Project", "");
        action.setMinLength(0);
        res.addOption(action);
        
        action = new StringConfigOption(CFG_VER, "Version", "");
        action.setMinLength(0);
        res.addOption(action);

        return res;
    }

    public ConfigResponse getConfigResponse()
        throws InvalidOptionException, InvalidOptionValueException 
    {
        ConfigResponse response = new ConfigResponse();
        response.setValue(CFG_META, getMeta());
        response.setValue(CFG_PROD, getProduct());
        response.setValue(CFG_VER, getVersion());
        return response;
    }

    public void setImplementor(String impl) {
        _implementor = impl;
    }

    public String getImplementor() {
        return _implementor;
    }
    
    public String getVersion() {
        return _version;
    }

    public void setVersion(String string) {
        _version = string;
    }

    public String getProduct() {
        return _product;
    }

    public void setProduct(String string) {
        _product = string;
    }

    public String getMeta() {
        return _meta;
    }

    public void setMeta(String string) {
        _meta = string;
    }
}
