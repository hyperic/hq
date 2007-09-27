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

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;

public abstract class LogFileTrackPlugin
    extends LogTrackPlugin {

    static final String PROP_FILES =
        ProductPlugin.TYPE_LOG_TRACK + ".files";

    private static final String[] FILES_PROPS =
        createTypeLabels(PROP_FILES);
    
    public static final String PROP_FILES_PLATFORM =
        FILES_PROPS[TypeInfo.TYPE_PLATFORM];
    
    public static final String PROP_FILES_SERVER =
        FILES_PROPS[TypeInfo.TYPE_SERVER];

    public static final String PROP_FILES_SERVICE =
        FILES_PROPS[TypeInfo.TYPE_SERVICE];
    
    protected String getDefaultLogFile(TypeInfo info, ConfigResponse config) {
        String file = getTypeProperty("DEFAULT_LOG_FILE");
        if (file == null) {
            return "";
        }
        if (info.isWin32Platform()) {
            file = StringUtil.replace(file, "/", "\\");
        }
        else {
            file = StringUtil.replace(file, "\\", "/");
        }
        return file;
    }
    
    protected ConfigOption getFilesOption(TypeInfo info, ConfigResponse config) {
        StringConfigOption option =
            new StringConfigOption(FILES_PROPS[info.getType()],
                                   "Log Files",
                                   getDefaultLogFile(info, config));
        option.setOptional(true);
        return option;
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        ConfigSchema schema = super.getConfigSchema(info, config);

        ConfigOption option = getFilesOption(info, config);
        if (option != null) {
            schema.addOption(option);
        }

        return schema;
    }

    public String[] getFiles(ConfigResponse config) {
        int type = getTypeInfo().getType();
        String files = config.getValue(FILES_PROPS[type]);
        String installpath =
            config.getValue(ProductPlugin.PROP_INSTALLPATH);
        return getAbsoluteFiles(files, installpath);
    }
}
