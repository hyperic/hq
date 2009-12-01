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
package org.hyperic.hq.livedata.formatters;

import org.hyperic.hq.livedata.FormatType;
import org.hyperic.hq.livedata.LiveDataFormatter;
import org.hyperic.hq.livedata.shared.LiveDataCommand;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.i18n.MessageBundle;

public class ToStringFormatter
    implements LiveDataFormatter
{
    private static final MessageBundle bundle = 
        MessageBundle.getBundle("org.hyperic.hq.livedata.Resources");
    private static final ConfigSchema EMPTY_SCHEMA = new ConfigSchema();
    
    public boolean canFormat(LiveDataCommand cmd, FormatType type) {
        return type.equals(FormatType.TEXT);
    }

    public String format(LiveDataCommand cmd, FormatType type, 
                         ConfigResponse formatCfg, Object val)
    {
        if (val.getClass().isArray()) {
            return ArrayUtil.toString((Object[])val);
        }
        return val.toString();
    }

    public ConfigSchema getConfig(LiveDataCommand cmd) {
        return EMPTY_SCHEMA;
    }

    public String getDescription() {
        return bundle.format("formatter.toString.desc");
    }

    public String getName() {
        return bundle.format("formatter.toString.name");
    }

    public String getId() {
        return "toString";
    }
}
