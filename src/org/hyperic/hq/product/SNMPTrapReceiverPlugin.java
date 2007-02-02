package org.hyperic.hq.product;

import java.io.IOException;

import org.hyperic.util.config.ConfigResponse;

public class SNMPTrapReceiverPlugin extends LogTrackPlugin {

    private boolean _isRegistered = false;

    private SNMPTrapReceiver getReceiver()
        throws IOException {

        return SNMPTrapReceiver.getInstance(getManager().getProperties());
    }

    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);

        try {
            getReceiver().add(this);
            _isRegistered = true;
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    public void shutdown() throws PluginException {
        super.shutdown();

        if (!_isRegistered) {
            return;
        }

        try {
            getReceiver().remove(this);
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }
}
