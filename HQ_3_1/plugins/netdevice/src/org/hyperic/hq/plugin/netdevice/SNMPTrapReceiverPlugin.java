package org.hyperic.hq.plugin.netdevice;

import java.io.IOException;

import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.util.config.ConfigResponse;

public class SNMPTrapReceiverPlugin extends LogTrackPlugin {

    private SNMPTrapReceiver getReceiver()
        throws IOException {

        return SNMPTrapReceiver.getInstance(getManager().getProperties());
    }

    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);

        try {
            getReceiver().add(this);
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    public void shutdown() throws PluginException {
        super.shutdown();

        SNMPTrapReceiver.remove(this);
    }
}
