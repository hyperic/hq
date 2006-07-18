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

import org.hyperic.util.config.BooleanConfigOption;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public abstract class ConfigTrackPlugin
    extends GenericPlugin {

    static final String PROP_ENABLE =
        ProductPlugin.TYPE_CONFIG_TRACK + ".enable";

    private static final String[] ENABLE_PROPS =
        createTypeLabels(PROP_ENABLE);

    private ConfigTrackPluginManager manager;

    public static boolean isEnabled(ConfigResponse config,
                                    int type) {
        String option = config.getValue(ENABLE_PROPS[type]);
        if (option == null) {
            return false;
        }

        return option.equals("true");
    }

    public static void setEnabled(ConfigResponse config, int type) {
        config.setValue(ENABLE_PROPS[type], "true");
    }

    protected ConfigOption getEnableOption(TypeInfo info, ConfigResponse config) {
        String defaultValue = getTypeProperty("DEFAULT_CONFIG_TRACK_ENABLE");
        boolean enable = "true".equals(defaultValue);

        ConfigOption option =
            new BooleanConfigOption(ENABLE_PROPS[info.getType()],
                                    "Enable Config Tracking",
                                    enable);
        option.setOptional(true);
        return option;
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        ConfigSchema schema = new ConfigSchema();

        ConfigOption enableOption = getEnableOption(info, config);
        if (enableOption != null) {
            schema.addOption(enableOption);
        }

        return schema;
    }

    public void init(PluginManager manager)
        throws PluginException {
        
        super.init(manager);
        
        this.manager = (ConfigTrackPluginManager)manager;
    }

    public ConfigTrackPluginManager getManager() {
        return this.manager;
    }
}
