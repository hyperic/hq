package org.hyperic.hq.product;

import java.io.IOException;

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

        try {
            getReceiver().remove(this);
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }
}
