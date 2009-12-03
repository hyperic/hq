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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

import org.hyperic.hq.appdef.shared.AIServerExtValue;

//wrapper class so plugins do not directly deal with AIServerExtValue

public class ServerResource {
    //package has access to these fields
    List services = new ArrayList();
    List serviceTypes = new ArrayList();

    private AIServerExtValue resource;
    private String[] connectProps = null;
    private String fqdn = null;
    private ConfigResponse productConfig=null;
    private ConfigResponse metricConfig=null;
    private ConfigResponse controlConfig=null;
    private ConfigResponse cprops=null;

    public ServerResource() {
        this.resource = new AIServerExtValue();
    }
    //XXX would prefer not to be public but required for autoinventory.ScanState
    public Object getResource() {
        if (this.connectProps != null) {
            if (this.metricConfig != null) {
                this.resource.addMetricConnectHashCode(this.metricConfig,
                                                       this.connectProps);
            }
            if (this.productConfig != null) {
                this.resource.addMetricConnectHashCode(this.productConfig,
                                                       this.connectProps);
            }
        }
        return this.resource;
    }
    /**
     * This attribute should only be set if discovered server is hosted
     * on a platform other than the platform which ran the auto discovery.
     * The WebLogic plugin for example may find cluster nodes on other
     * platforms.  Note that setting this attribute will also require the
     * given platform to exist in the HQ inventory.
     */
    public void setPlatformFqdn(String name) {
        this.fqdn = name;
    }
    
    public String getPlatformFqdn() {
        return this.fqdn;
    }
    
    public void setConnectProperties(String[] keys) {
        this.connectProps = keys;
    }
    
    public String[] getConnectProperties() {
        return this.connectProps;
    }

    public void addService(ServiceResource service) {
        //service.resource.setCTime(...); XXX?
        this.services.add(service.resource);
    }
    
    public void addServiceType(ServiceType serviceType) {
    	this.serviceTypes.add(serviceType.getAIServiceTypeValue());
    }

    public void setInstallPath(String name) {
        this.resource.setInstallPath(name);
    }

    public String getInstallPath() {
        return this.resource.getInstallPath();
    }

    public void setIdentifier(String name) {
        this.resource.setAutoinventoryIdentifier(name);
    }

    public String getIdentifier() {
        return this.resource.getAutoinventoryIdentifier();
    }

    public void setType(GenericPlugin plugin) {
        this.resource.setServerTypeName(plugin.getTypeInfo().getName());
    }

    //following methods are the same as ServiceResource's
    public void setType(String name) {
        this.resource.setServerTypeName(name);
    }

    public String getType() {
        return this.resource.getServerTypeName();
    }
    
    public void setName(String name) {
        this.resource.setName(name);
    }

    public String getName() {
        return this.resource.getName();
    }

    public void setDescription(String description) {
        this.resource.setDescription(description);
    }

    public String getDescription() {
        return this.resource.getDescription();
    }
    
    private RuntimeException encodeException() {
        return new RuntimeException("Error encoding config");
    }

    public ConfigResponse getProductConfig() {
        return this.productConfig;
    }

    public void setProductConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }
        this.productConfig = config;
        try {
            this.resource.setProductConfig(config.encode());
        } catch (EncodingException e) {
            throw encodeException();
        }
    }

    public ConfigResponse getMeasurementConfig() {
        return this.metricConfig;
    }

    public void setMeasurementConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }
        this.metricConfig = config;
        try {
            this.resource.setMeasurementConfig(config.encode());
        } catch (EncodingException e) {
            throw encodeException();
        }
    }

    public void setMeasurementConfig(ConfigResponse config,
                                     int logTrackLevel,
                                     boolean enableConfigTrack) {
        LogTrackPlugin.setEnabled(config,
                                  TypeInfo.TYPE_SERVICE,
                                  logTrackLevel);
        if (enableConfigTrack) {
            ConfigTrackPlugin.setEnabled(config, TypeInfo.TYPE_SERVICE);
        }
        setMeasurementConfig(config);
    }
    
    public ConfigResponse getControlConfig() {
        return this.controlConfig;
    }

    public void setControlConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }
        this.controlConfig = config;
        try {
            this.resource.setControlConfig(config.encode());
        } catch (EncodingException e) {
            throw encodeException();
        }
    }

    public void setProductConfig() {
        this.resource.setProductConfig(ConfigResponse.EMPTY_CONFIG);
    }

    public void setMeasurementConfig() {
        this.resource.setMeasurementConfig(ConfigResponse.EMPTY_CONFIG);
    }

    public void setControlConfig() {
        this.resource.setControlConfig(ConfigResponse.EMPTY_CONFIG);
    }

    public ConfigResponse getCustomProperties() {
        return this.cprops;
    }

    public void setCustomProperties(ConfigResponse config) {
        if (config == null) {
            return;
        }
        this.cprops = config;
        try {
            this.resource.setCustomProperties(config.encode());
        } catch (EncodingException e) {
            throw encodeException();
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

    public String toString() {
        return this.resource.toString();
    }
}
