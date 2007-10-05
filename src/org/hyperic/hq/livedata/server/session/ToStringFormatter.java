package org.hyperic.hq.livedata.server.session;

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
    
    public boolean canFormat(LiveDataCommand cmd) {
        return true;
    }

    public String format(LiveDataCommand cmd, ConfigResponse formatCfg,
                         Object val)
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
}
