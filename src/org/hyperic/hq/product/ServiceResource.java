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

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.EncodingException;

import org.hyperic.hq.appdef.shared.AIServiceValue;

//wrapper class so plugins do not directly deal with AIServiceValue

public class ServiceResource {
    //package has access to this field
    AIServiceValue resource;

    /**
     * Special token when used in the service name will be replaced
     * with the service's parent server name.
     */
    public static final String SERVER_NAME_PREFIX = "%serverName%" + " ";

    public ServiceResource() {
        this.resource = new AIServiceValue();
    }

    /**
     * Set the name of the service, prepending the parent server name
     * using SERVER_NAME_PREFIX.
     * @param name Service name, must be unique to the parent server.
     */
    public void setServiceName(String name) {
        setName(SERVER_NAME_PREFIX + name);
    }
    
    public void setType(GenericPlugin plugin, String type) {
        setType(plugin.getTypeInfo().getName() + " " + type);
    }

    //following methods are the same as ServiceResource's
    public void setType(String name) {
        this.resource.setServiceTypeName(name);
    }
    
    public String getType() {
        return this.resource.getServiceTypeName();
    }

    /**
     * In most cases, setServiceName should be used to include
     * the parent server name for uniqueness.
     */
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

    public void setProductConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }
        try {
            this.resource.setProductConfig(config.encode());
        } catch (EncodingException e) {
            throw encodeException();
        }
    }

    public void setMeasurementConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }
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

    public void setControlConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }
        try {
            this.resource.setControlConfig(config.encode());
        } catch (EncodingException e) {
            throw encodeException();
        }
    }

    public void setResponseTimeConfig(ConfigResponse config) {
        if (config == null) {
            return;
        }
        try {
            this.resource.setResponseTimeConfig(config.encode());
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

    public void setCustomProperties(ConfigResponse config) {
        if ((config == null) || (config.getKeys().size() == 0)) {
            return;
        }
        try {
            this.resource.setCustomProperties(config.encode());
        } catch (EncodingException e) {
            throw encodeException();
        }
    }
    
    public String toString() {
        return this.resource.toString();
    }
}
