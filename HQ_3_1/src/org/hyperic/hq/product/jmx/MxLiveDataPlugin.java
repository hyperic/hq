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

package org.hyperic.hq.product.jmx;

import org.hyperic.hq.product.LiveDataPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.StringUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.openmbean.CompositeData;
import java.util.Properties;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;

public class MxLiveDataPlugin extends LiveDataPlugin {

    private static Log _log = LogFactory.getLog(MxLiveDataPlugin.class);

    private static final String PROP_OBJNAME   = "ObjectName";
    private static final String PROP_METHOD    = "Method";
    private static final String PROP_ATTRIBUTE = "Attribute";
    private static final String PROP_ARGS      = "Arguments";

    private static final String CMD_GET    = "get";
    private static final String CMD_INVOKE = "invoke";

    private static final String[] _COMMANDS = {
        CMD_GET,
        CMD_INVOKE
    };

    public Object getData(String command, ConfigResponse config)
        throws PluginException
    {
        Properties props = config.toProperties();
        _log.debug("Using properties: " + props);

        try {
            String oName = props.getProperty(PROP_OBJNAME);
            
            Object res;
            if (command.equals(CMD_INVOKE)) {
                String method = props.getProperty(PROP_METHOD);
                if (method == null) {
                    throw new PluginException("No method given.");
                }

                String argsStr = props.getProperty(PROP_ARGS, "");
                List args = StringUtil.explode(argsStr, ",");
                _log.debug("Using arguments:" + args);

                res = MxUtil.invoke(props, oName, method,
                                    args.toArray(new String[0]), new String[0]);
            } else if (command.equals(CMD_GET)) {
                String attribute = props.getProperty(PROP_ATTRIBUTE);
                if (attribute == null) {
                    throw new PluginException("No attribute given");
                }

                res = MxUtil.getValue(props, oName, attribute);
            } else {
                throw new PluginException("Unknown command " + command);
            }

            // if CompositeData, return Map for simplicity.
            if (res instanceof CompositeData) {
                return convertCompositeData((CompositeData)res);
            }

            // Same goes for CompositeData[]
            if (res instanceof CompositeData[]) {
                CompositeData[] o = (CompositeData[])res;
                Map[] ret = new Map[o.length];
                for (int i = 0; i < o.length; i++) {
                    ret[i] = convertCompositeData(o[i]);
                }
                return ret;
            }
            
            return res;

        } catch (Exception e) {
            throw new PluginException(e);
        }
    }

    private Map convertCompositeData(CompositeData data) {
        Map retval = new HashMap();
        for (Iterator i = data.getCompositeType().keySet().iterator();
             i.hasNext(); ) {
            String key = (String)i.next();
            Object val = data.get(key);
            retval.put(key, val);
        }
        return retval;
    }

    public String[] getCommands() {
        return _COMMANDS;
    }

    public ConfigSchema getConfigSchema(String command)
        throws PluginException
    {
        ConfigSchema schema = new ConfigSchema();

        StringConfigOption objectName =
            new StringConfigOption(PROP_OBJNAME, "Object name");
        schema.addOption(objectName);

        if (command.equals(CMD_GET)) {
            StringConfigOption attr =
                new StringConfigOption(PROP_ATTRIBUTE, "Attribute to get");
            schema.addOption(attr);
        } else if (command.equals(CMD_INVOKE)) {
            StringConfigOption method =
                new StringConfigOption(PROP_METHOD, "Method to invoke");
            schema.addOption(method);

            StringConfigOption args =
                new StringConfigOption(PROP_ARGS, "Comma separated arguments", 
                                       "");
            args.setOptional(true);
            schema.addOption(args);
        } else {
            throw new PluginException("Unknown command " + command);
        }

        return schema;
    }
}
