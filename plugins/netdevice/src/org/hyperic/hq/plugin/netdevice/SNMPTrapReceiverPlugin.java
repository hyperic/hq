package org.hyperic.hq.plugin.netdevice;

import java.io.IOException;

import org.hyperic.hq.product.LogTrackPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.PluginManager;
import org.hyperic.util.config.ConfigResponse;

public class SNMPTrapReceiverPlugin extends LogTrackPlugin {

    private SNMPTrapReceiver getReceiver()
        throws IOException {

        SNMPTrapReceiver receiver =
            SNMPTrapReceiver.getInstance(getManager().getProperties());
        receiver.setPluginManager(getManager());
        return receiver;
    }

    public void init(PluginManager manager) throws PluginException {
        super.init(manager);
        if (SNMPTrapReceiver.hasInstance()) {
            return;
        }
        String listen =
            manager.getProperty(SNMPTrapReceiver.PROP_LISTEN_ADDRESS);
        if (listen == null) {
            return;
        }
        getLog().debug("Configuring default listener: " + listen);
        try {
            getReceiver();
        } catch (Exception e) {
            throw new PluginException(e.getMessage(), e);
        }
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
