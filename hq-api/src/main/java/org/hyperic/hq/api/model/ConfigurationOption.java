/* **********************************************************************
/*
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004-2010], VMware, Inc.
 * This file is part of Hyperic.
 *
 * Hyperic is free software; you can redistribute it and/or modify
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
 *
 * **********************************************************************
 * 29 April 2012
 * Maya Anderson
 * *********************************************************************/
package org.hyperic.hq.api.model;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "configurationOption", namespace=RestApiConstants.SCHEMA_NAMESPACE)
@XmlType(name="ConfigurationOptionType", namespace=RestApiConstants.SCHEMA_NAMESPACE)
public class ConfigurationOption {

    @XmlAttribute
    private String     name;
    @XmlAttribute
    private String     description;  
    @XmlAttribute
    private String     category; 
    @XmlAttribute    
    private String     defaultValue;
    @XmlAttribute
    private String     confirmWithValue;    // The value to double check on
    @XmlAttribute
    private Boolean    isOptional;
    @XmlAttribute
    private String     type;    // { int, double, boolean, long, string, ip, enum, secret, hidden, port, macaddress, stringarray};
    
    @XmlElementWrapper(name="enumValues", namespace=RestApiConstants.SCHEMA_NAMESPACE)
    @XmlElement(name = "enumValue", namespace=RestApiConstants.SCHEMA_NAMESPACE, nillable=true)
    private List<String> enumValues;

    public ConfigurationOption() {
    }

    public ConfigurationOption(String name, String description, String category, String defaultValue,
            String confirmWithValue, Boolean isOptional, String type, List<String> enumValues) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.defaultValue = defaultValue;
        this.confirmWithValue = confirmWithValue;
        this.isOptional = isOptional;
        this.type = type;
        this.enumValues = enumValues;
    }    
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getConfirmWithValue() {
        return confirmWithValue;
    }

    public void setConfirmWithValue(String confirmWithValue) {
        this.confirmWithValue = confirmWithValue;
    }

    public Boolean getIsOptional() {
        return isOptional;
    }

    public void setIsOptional(Boolean isOptional) {
        this.isOptional = isOptional;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(List<String> enumValues) {
        this.enumValues = enumValues;
    }    
 

}
