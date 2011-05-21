package org.hyperic.hq.agent.bizapp.client;

import java.util.Collection;

import org.hyperic.hq.agent.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.PluginReport_args;
import org.hyperic.hq.product.PluginInfo;

public class PlugininventoryCallback extends AgentCallback {

    private final PluginReport_args args;

    public PlugininventoryCallback(ProviderFetcher fetcher, Collection<PluginInfo> plugins) {
        super(fetcher);
        args = new PluginReport_args(plugins);
        String agentToken = fetcher.getProvider().getAgentToken();
        args.setAgentToken(agentToken);
    }
    
    public void sendPluginReportToServer() throws AgentCallbackException {
        ProviderInfo provider = getProvider();
        invokeLatherCall(provider, CommandInfo.CMD_PLUGIN_SEND_REPORT, args);
    }

}
