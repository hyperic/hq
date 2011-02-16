package org.hyperic.hq.bizapp.client;

import java.util.Collection;

import org.hyperic.hq.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.PluginReport_args;
import org.hyperic.hq.product.PluginInfo;

public class PlugininventoryCallbackClient extends AgentCallbackClient {

    private final PluginReport_args args;

    public PlugininventoryCallbackClient(ProviderFetcher fetcher, Collection<PluginInfo> plugins) {
        super(fetcher);
        args = new PluginReport_args(plugins);
        String agentToken = fetcher.getProvider().getAgentToken();
        args.setAgentToken(agentToken);
    }
    
    public void sendPluginReportToServer() throws AgentCallbackClientException {
        ProviderInfo provider = getProvider();
        invokeLatherCall(provider, CommandInfo.CMD_PLUGIN_SEND_REPORT, args);
    }

}
