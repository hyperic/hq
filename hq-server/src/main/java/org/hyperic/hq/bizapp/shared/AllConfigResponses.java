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

package org.hyperic.hq.bizapp.shared;

import java.io.Serializable;

import org.hyperic.hq.appdef.shared.AppdefEntityID;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

/**
 * This is a simple class that wraps all the config responses.  It is used
 * when calling the AppdefBoss.setAllConfigResponses
 */
public class AllConfigResponses implements Serializable {

    private AppdefEntityID _resource;

    private ConfigResponse[] _configs
        = new ConfigResponse[ProductPlugin.CONFIGURABLE_TYPES.length];
    private boolean[] _supports
        = new boolean[ProductPlugin.CONFIGURABLE_TYPES.length];
    private boolean[] _shouldConfig
        = new boolean[ProductPlugin.CONFIGURABLE_TYPES.length];

    private boolean _enableRuntimeAIScan;
    private boolean _enableEuRT;
    private boolean _enableServiceRT;

    public AllConfigResponses () {}

    public AppdefEntityID setResource(AppdefEntityID resource) {
        _resource = resource;
        return resource  ;
    }

    public AppdefEntityID getResource() {
        return _resource;
    }

    public void setConfig(int type, ConfigResponse config) {
        _configs[type] = config;
    }

    public ConfigResponse getConfig(int type) {
        return _configs[type];
    }

    public void setSupports(int type, boolean b) {
        _supports[type] = b;
    }

    public boolean getSupports(int type) {
        return _supports[type];
    }

    public void setShouldConfig(int type, boolean b) {
        _shouldConfig[type] = b;
    }

    public boolean getShouldConfig(int type) {
        return _shouldConfig[type];
    }

    public void setEnableRuntimeAIScan(boolean aiScan) {
        _enableRuntimeAIScan = aiScan;
    }

    public boolean getEnableRuntimeAIScan() {
        return _enableRuntimeAIScan;
    }

    public void setEnableEuRT (boolean eurt) {
        _enableEuRT = eurt;
    }

    public boolean getEnableEuRT() {
        return _enableEuRT;
    }

    public void setEnableServiceRT(boolean serviceRt) {
        _enableServiceRT = serviceRt;
    }

    public boolean getEnableServiceRT() {
        return _enableServiceRT;
    }

    // Convenience methods
    public ConfigResponse getProductConfig() {
        return getConfig(ProductPlugin.CFGTYPE_IDX_PRODUCT);
    }

    public ConfigResponse getMetricConfig() {
        return getConfig(ProductPlugin.CFGTYPE_IDX_MEASUREMENT);
    }

    public ConfigResponse getControlConfig() {
        return getConfig(ProductPlugin.CFGTYPE_IDX_CONTROL);
    }

    public ConfigResponse getRtConfig() {
        return getConfig(ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME);
    }

    public boolean supportsProductConfig() {
        return getSupports(ProductPlugin.CFGTYPE_IDX_PRODUCT);
    }

    public boolean supportsMetricConfig() {
        return getSupports(ProductPlugin.CFGTYPE_IDX_MEASUREMENT);
    }

    public boolean supportsControlConfig() {
        return getSupports(ProductPlugin.CFGTYPE_IDX_CONTROL);
    }

    public boolean supportsRtConfig() {
        return getSupports(ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME);
    }

    public boolean shouldConfigProduct() {
        return getShouldConfig(ProductPlugin.CFGTYPE_IDX_PRODUCT);
    }

    public boolean shouldConfigMetric() {
        return getShouldConfig(ProductPlugin.CFGTYPE_IDX_MEASUREMENT);
    }

    public boolean shouldConfigControl() {
        return getShouldConfig(ProductPlugin.CFGTYPE_IDX_CONTROL);
    }

    public boolean shouldConfigRt() {
        return getShouldConfig(ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME);
    }

    public String toString () {
        return "[AllConfigResponses resource=" + _resource
            + " configs=" + StringUtil.arrayToString(_configs)
            + " supports=" + StringUtil.arrayToString(_supports)
            + " shouldConfig=" + StringUtil.arrayToString(_shouldConfig)
            + " enableRuntimeAI=" + _enableRuntimeAIScan
            + " enableEuRT=" + _enableEuRT
            + " enableServiceRT=" + _enableServiceRT + "]";
    }
}
