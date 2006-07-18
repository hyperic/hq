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

package org.hyperic.hq.product.logparse;

import org.hyperic.hq.product.TypeInfo;
import org.hyperic.hq.product.RtPlugin;
import org.hyperic.hq.product.ProductPlugin;

import org.hyperic.util.config.SchemaBuilder;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

/* This RT plugin uses the SimpleLogParser to parse all RT logs.
 * preferably, all RT plugins will become a sub-class of this
 * plugin.
 */
public abstract class SimpleRtPlugin extends RtPlugin
{
    double tm;

    /* The simple log parser doesn't need a log format.  We just use a
     * stringTokenizeer.  Unfortunately, we need these methods for the
     * RtPluginManager, so we just return an empty string.
     */

    public String convertFormat(String fmt) {
        return fmt;
    }

    public double getTimeMultiplier() {
        return 1;
    }

    public BaseLogParser getParser()
    {
        return SimpleLogParser.getInstance();
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config)
    {
        int type = info.getType();

        if (type != TypeInfo.TYPE_SERVICE) {
            return new ConfigSchema();
        }

        SchemaBuilder schema = new SchemaBuilder(config);

        String serverRoot = config.getValue(ProductPlugin.PROP_INSTALLPATH);

        schema.add(CONFIG_LOGDIR, "full path to log directory",
                serverRoot + "/logs");
        schema.add(CONFIG_LOGMASK,
                "the filenames of your log files with wildcards",
                "rt_log");
        schema.add(CONFIG_INTERVAL, "Interval between parsing log files " +
                "(seconds)",
                60);

        if (supportsEndUser()) {
            schema.add(CONFIG_EULOGDIR, "full path to end user log directory",
                    serverRoot + "/logs");
            schema.add(CONFIG_EULOGMASK,
                    "the filenames of your end user log files with wildcards",
                    "enduser_log");
            schema.add(CONFIG_EUINTERVAL, 
                    "Interval between parsing end user log files (seconds)",
                    60);
        }
        schema.addRegex(CONFIG_TRANSFORM,
                "Regular expressions to apply to all URLS, space separated",
                null).setOptional(true);

        schema.addStringArray(CONFIG_DONTLOG,
                "Regular expressions specifying which URLs not to log, " +
                "space separated", null).setOptional(true);

        return schema.getSchema();
    }

}
