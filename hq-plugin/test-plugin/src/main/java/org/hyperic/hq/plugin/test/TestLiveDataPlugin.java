package org.hyperic.hq.plugin.test;

import org.hyperic.hq.product.LiveDataPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

public class TestLiveDataPlugin extends LiveDataPlugin{

    @Override
    public String[] getCommands() {
        return new String[] {"someCommand"};
    }

    @Override
    public Object getData(String command, ConfigResponse config) throws PluginException {
        return new CommandResult("testing");
    }

}
