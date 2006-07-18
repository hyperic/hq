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
    
    private final static String CFG_META = "meta";
    private final static String CFG_PROD = "product";
    private final static String CFG_VER  = "version";
    
    private static String implementor =
        "org.hyperic.hq.bizapp.server.action.log.SyslogAction";
    
    private String meta;
    private String product;
    private String version;
    
    public SyslogActionConfig(){
    }

    public void init(ConfigResponse config){
        this.setMeta(config.getValue(CFG_META));
        this.setProduct(config.getValue(CFG_PROD));
        this.setVersion(config.getValue(CFG_VER));
    }
    
    public ConfigSchema getConfigSchema(){
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

    /* (non-Javadoc)
     * @see org.hyperic.hq.events.ext.ActionInterface#getConfigResponse()
     */
    public ConfigResponse getConfigResponse()
        throws InvalidOptionException, InvalidOptionValueException {
        ConfigResponse response = new ConfigResponse();
        response.setValue(CFG_META, this.getMeta());
        response.setValue(CFG_PROD, this.getProduct());
        response.setValue(CFG_VER, this.getVersion());
        return response;
    }

    /**
     * Set the name of the action class
     * @param impl the name of the implementing class
     */
    public void setImplementor(String impl) {
        implementor = impl;
    }

    /**
     * Get the name of the action class
     * @return the name of the implementing class
     */
    public String getImplementor() {
        return implementor;
    }
    
    /**
     * @return
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param string
     */
    public void setVersion(String string) {
        version = string;
    }

    /**
     * @return
     */
    public String getProduct() {
        return product;
    }

    /**
     * @param string
     */
    public void setProduct(String string) {
        product = string;
    }

    /**
     * @return
     */
    public String getMeta() {
        return meta;
    }

    /**
     * @param string
     */
    public void setMeta(String string) {
        meta = string;
    }

}
