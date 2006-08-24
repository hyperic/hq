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

import java.util.ArrayList;
import java.util.List;

import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;
import org.hyperic.util.xmlparser.XmlAttrException;
import org.hyperic.util.xmlparser.XmlEndAttrHandler;
import org.hyperic.util.xmlparser.XmlTagException;
import org.hyperic.util.xmlparser.XmlTagInfo;

class ConfigTag extends BaseTag implements XmlEndAttrHandler {

    private static final String[] OPTIONAL_ATTRS = {
        ATTR_TYPE, ATTR_NAME, ATTR_PLATFORM, ATTR_INCLUDE
    };
    
    SchemaBuilder schema;
    private List unresolved;

    ConfigTag(BaseTag parent) {
        super(parent);
    }

    public String getName() {
        return "config";
    }

    public String[] getOptionalAttributes() {
        return OPTIONAL_ATTRS;
    }

    protected ConfigOptionTag getConfigOptionTag() {
        return new ConfigOptionTag(this);
    }
    
    public XmlTagInfo[] getSubTags() {
        return new XmlTagInfo[] {
            new XmlTagInfo(getConfigOptionTag(),
                           XmlTagInfo.ZERO_OR_MORE)
        };
    }

    public void startTag() {
        super.startTag();
        this.schema = new SchemaBuilder();
        this.unresolved = new ArrayList();
    }
    
    private ConfigSchema getConfigSchema(String include) {
        SchemaBuilder includeSchema =
            (SchemaBuilder)getScratch(include);
        if (includeSchema != null) {
            return includeSchema.getSchema();
        }
        return PluginData.getSharedConfigSchema(include);
    }

    static ConfigOption includeConfigOption(ConfigOption option, String defVal) {
        if (defVal != null) {
            try {
                ConfigOption copy = option.copy();
                copy.setDefault(defVal);
                return copy;
            } catch (CloneNotSupportedException e) {
                e.printStackTrace(); //XXX
            }
        }
        return option;
    }

    private ConfigOption includeConfigOption(ConfigOption option) {
        if (!isResourceParent()) {
            return option;
        }
        ResourceTag resource = (ResourceTag)this.parent; 
        //allow <property> to override default value
        String defVal =
            resource.getAttribute(option.getName());

        return includeConfigOption(option, defVal);
    }
    
    public void endAttributes() throws XmlAttrException {
        ConfigSchema schema = this.schema.getSchema();
        String[] includes = getAttributeList(ATTR_INCLUDE);

        if (isResourceParent()) {
            ResourceTag resource =
                (ResourceTag)this.parent;
            if (includes.length == 1) {
                //resource has <config include="foo"/>
                resource.configName = includes[0];
            }
            else {
                resource.configName = null;
            }
        }

        for (int i=0; i<includes.length; i++) {
            ConfigSchema includeSchema = getConfigSchema(includes[i]);
            if (includeSchema == null) {
                this.unresolved.add(includes[i]);
                continue;
            }
            List options = includeSchema.getOptions();
            for (int j=0; j<options.size(); j++) {
                ConfigOption option =
                    (ConfigOption)options.get(j);

                schema.addOption(includeConfigOption(option));
            }
        }
    }

    protected void addConfigSchema(String typeName, int idx,
                                   ConfigSchema schema) {
        this.data.addConfigSchema(typeName, idx, schema);
    }

    public void endTag() throws XmlTagException {
        String name = getAttribute(ATTR_NAME);
        String type = getAttribute(ATTR_TYPE);
        boolean isGlobal = isGlobalType();
        
        if (!isResourceParent()) {
            if (name == null) {
                String msg = getName() + " name attribute required here";
                throw new XmlTagException(msg);
            }
        }

        if (name != null) {
            if (isGlobal) {
                type = null;
            }
            if (type != null) {
                String msg =
                    "config type " + type + " not allowed with named config";
                throw new XmlTagException(msg);
            }
            if (getAttribute(ATTR_PLATFORM) != null) {
                String msg =
                    "config platform attribute not allowed with named config";
                throw new XmlTagException(msg);
            }

            putScratch(name, this.schema);

            //generate a ${name}.config and ${name}.template based on the schema
            ConfigSchema configSchema = this.schema.getSchema();
            StringBuffer sb = new StringBuffer();
            String[] options = configSchema.getOptionNames();
            for (int i=0; i<options.length; i++) {
                String key = options[i];
                sb.append(key).
                    append('=').
                    append('%').append(key).append('%');
                if (i < options.length-1) {
                    sb.append(',');
                }
            }

            String template = sb.toString();
            String[][] props = {
                { name + ".config", template },
                { name + ".template", name + ":" + template },
            };

            if (isGlobal) {
                for (int i=0; i<props.length; i++) {
                    this.data.setGlobalProperty(props[i][0], props[i][1]);
                }
                PluginData.addSharedConfigSchema(name, configSchema);
            }
            else {
                for (int i=0; i<props.length; i++) {
                    this.data.addFilter(props[i][0], props[i][1]);  
                }
            }
            return;
        }

        int idx;
        if ((type == null) || type.equals(ProductPlugin.TYPE_PRODUCT)) {
            idx = ProductPlugin.CFGTYPE_IDX_PRODUCT;
        }
        else if (type.equals(ProductPlugin.TYPE_MEASUREMENT)) {
            idx = ProductPlugin.CFGTYPE_IDX_MEASUREMENT;
        }
        else if (type.equals(ProductPlugin.TYPE_CONTROL)) {
            idx = ProductPlugin.CFGTYPE_IDX_CONTROL;
        }
        else if (type.equals(ProductPlugin.TYPE_RESPONSE_TIME)) {
            idx = ProductPlugin.CFGTYPE_IDX_RESPONSE_TIME;
        }
        else {
            throw new XmlTagException("Unsupported config type: " + type);
        }
        
        ResourceTag resource = (ResourceTag)this.parent;
        ConfigSchema schema = this.schema.getSchema();
        if (this.unresolved.size() != 0) {
            List options = schema.getOptions();
            schema = new LateBindingConfigSchema(this.unresolved,
                                                 resource.props);
            schema.addOptions(options);
        }
        String typeName = resource.getPlatformName(this);

        addConfigSchema(typeName, idx, schema);

        this.schema = null;
        super.endTag();
    }
}
