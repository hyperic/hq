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

import java.util.List;
import java.util.Properties;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigSchema;

//allows plugins to include ConfigSchema from other plugins
//regardless of order in which plugins are loaded.
//example: apache plugin does <config include="url"/>
//but netservices plugin defines the global url config
//getConfigSchema() is not called until after the plugins
//are all loaded at which point the include can be resolved.
class LateBindingConfigSchema extends ConfigSchema {

    private List includes;
    private Properties props;

    LateBindingConfigSchema(List includes, Properties props) {
        super();
        this.includes = includes;
        this.props = new Properties();
        this.props.putAll(props);
    }

    public List getOptions() {
        int size = this.includes.size(); 
        if (size != 0) {
            for (int i=0; i<size; i++) {
                String name = (String)this.includes.get(i);
                ConfigSchema schema =
                    PluginData.getSharedConfigSchema(name);
                if (schema == null) {
                    String msg =
                        "Unable to resolve config include=" + name;
                    throw new IllegalArgumentException(msg);
                }

                List options = schema.getOptions();
                for (int j=0; j<options.size(); j++) {
                    ConfigOption option =
                        (ConfigOption)options.get(j);

                    if (getOption(option.getName()) == null) {
                        String defVal =
                            this.props.getProperty(option.getName());
                        option =
                            ConfigTag.includeConfigOption(option, defVal);
                        addOption(option);
                    } //else plugin has overridden this option (e.g. snmpPort)
                }
            }
            this.includes.clear(); //one and done.
            this.props.clear();
            this.props = null;
        }

        return super.getOptions();
    }
}
