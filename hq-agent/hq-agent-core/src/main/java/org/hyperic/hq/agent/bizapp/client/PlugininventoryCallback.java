package org.hyperic.hq.agent.bizapp.client;

import org.hyperic.hq.agent.bizapp.agent.ProviderInfo;
import org.hyperic.hq.bizapp.shared.lather.CommandInfo;
import org.hyperic.hq.bizapp.shared.lather.PluginReport_args;
import org.hyperic.hq.product.PluginInfo;
import org.springframework.stereotype.Component;

import java.util.Collection;

@Component
public class PlugininventoryCallback extends AgentCallback {

    private PluginReport_args args;

    /*public PlugininventoryCallback() {}

    public PlugininventoryCallback(ProviderFetcher fetcher, Collection<PluginInfo> plugins) {
        super(fetcher);
        this.args = new PluginReport_args(plugins);
        String agentToken = fetcher.getProvider().getAgentToken();
        args.setAgentToken(agentToken);
    }*/

    public void initializePlugins(Collection<PluginInfo> plugins) {
        this.args = new PluginReport_args(plugins);
        String agentToken = providerFetcher.getProvider().getAgentToken();
        args.setAgentToken(agentToken);
    }
    
    public void sendPluginReportToServer() throws AgentCallbackException {
        ProviderInfo provider = getProvider();
        invokeLatherCall(provider, CommandInfo.CMD_PLUGIN_SEND_REPORT, args);
    } 
}
