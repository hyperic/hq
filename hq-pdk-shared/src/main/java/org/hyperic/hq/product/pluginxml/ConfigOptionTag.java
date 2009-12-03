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

package org.hyperic.hq.product.pluginxml;

import org.hyperic.hq.product.Metric;
import org.hyperic.util.config.BooleanConfigOption;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.HiddenConfigOption;
import org.hyperic.util.config.IpAddressConfigOption;
import org.hyperic.util.config.MacAddressConfigOption;
import org.hyperic.util.config.SchemaBuilder;
import org.hyperic.util.xmlparser.XmlTagException;

class ConfigOptionTag extends ContainerTag {

    static final String ATTR_DEFAULT  = "default";
    static final String ATTR_OPTIONAL = "optional";
    static final String ATTR_CATEGORY = "category";

    private static final String[] REQUIRED_ATTRS = {
        ATTR_NAME, ATTR_DESCRIPTION
    };
    
    private static final String[] OPTIONAL_ATTRS = {
        ATTR_DEFAULT, ATTR_TYPE, ATTR_OPTIONAL, ATTR_CATEGORY
    };
    
    private ConfigTag config;
    protected SchemaBuilder schema;
    
    ConfigOptionTag(BaseTag config) {
        super(config);
        this.config = (ConfigTag)config;
    }
    
    public String getName() {
        return "option";
    }
    
    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }

    public String[] getRequiredAttributes() {
        return REQUIRED_ATTRS;
    }
    
    public void startTag() {
        super.startTag();
        this.schema = this.config.schema;
    }

    private Integer integerValueOf(String val) {
        if (val == null) {
            return null;
        }
        return Integer.valueOf(val);
    }
    
    private Double doubleValueOf(String val) {
        if (val == null) {
            return null;
        }
        return Double.valueOf(val);
    }

    private String[] getIncludeOptions() {
        String[] options = new String[this.includes.size()];
        this.includes.toArray(options);
        this.includes.clear();
        return options;
    }

    private String getIncludeOptionsAsString(char delim) {
        StringBuffer buffer = new StringBuffer();
        int size = this.includes.size();
        for (int i=0; i<size; i++) {
            buffer.append(this.includes.get(i));
            if (i < size-1) {
                buffer.append(delim);
            }
        }
        this.includes.clear();
        return buffer.toString();
    }
    
    void endTag() throws XmlTagException {
        ConfigOption option;

        String type = getAttribute(ATTR_TYPE, "string");
        String name = getAttribute(ATTR_NAME);
        String dval = getAttribute(ATTR_DEFAULT);
        String desc = getAttribute(ATTR_DESCRIPTION, name);

        if (type.equals("string")) {
            option = this.schema.add(name, desc, dval);
        }
        else if (type.equals("int")) {
            option = this.schema.add(name, desc, integerValueOf(dval));
        }
        else if (type.equals("double")) {
            option = this.schema.add(name, desc, doubleValueOf(dval));
        }
        else if (type.equals("secret")) {
            Metric.addSecret(name);
            option = this.schema.addSecret(name, desc);
        }
        else if (type.equals("hidden")) {
            option = new HiddenConfigOption(name, dval);
            this.schema.getSchema().addOption(option);
        }
        else if (type.equals("port")) {
            option = this.schema.addPort(name, desc, integerValueOf(dval));
        }
        else if (type.equals("boolean")) {
            if (dval == null) {
                dval = "true";
            }
            option = new BooleanConfigOption(name, desc, dval.equals("true"));
            option.setOptional(true);
            this.schema.getSchema().addOption(option);
        }
        else if (type.equals("ipaddress")) {
            option = new IpAddressConfigOption(name, desc, dval);
            this.schema.getSchema().addOption(option);
        }
        else if (type.equals("macaddress")) {
            option = new MacAddressConfigOption(name, desc, dval);
            this.schema.getSchema().addOption(option);
        }
        else if (type.equals("enum")) {
            option =
                this.schema.addEnum(name, desc, getIncludeOptions(), dval);
        }
        else if (type.equals("stringarray")) {
            option =
                this.schema.addStringArray(name, desc,
                                           getIncludeOptionsAsString(' '));
        }
        //XXX there are more options
        else {
            throw new XmlTagException("Unsupported config type: " + type);
        }
        
        if (this.includes.size() > 0) {
            throw new XmlTagException("option type=" + type +
                                      " does support <include>");
        }

        if ("true".equals(getAttribute(ATTR_OPTIONAL))) {
            option.setOptional(true);
        }

        option.setCategory(getAttribute(ATTR_CATEGORY));

        super.endTag();
    }
}
