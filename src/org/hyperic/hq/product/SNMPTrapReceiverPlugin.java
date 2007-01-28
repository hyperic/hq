package org.hyperic.hq.product;

import java.io.IOException;

public class SNMPTrapReceiverPlugin extends LogTrackPlugin {

    private SNMPTrapReceiver getReceiver()
        throws IOException {

        return SNMPTrapReceiver.getInstance(getManager().getProperties());
    }

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);

        try {
            getReceiver().add(this);
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }

    public void shutdown() throws PluginException {
        super.shutdown();

        try {
            getReceiver().add(this);
        } catch (IOException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }
}
