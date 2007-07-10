package org.hyperic.hq.bizapp.client.shell;

import org.hyperic.hq.bizapp.client.shell.ClientShell_resource;
import org.hyperic.hq.bizapp.client.shell.ClientShell_resource_configure;
import org.hyperic.hq.product.ProductPlugin;

public class ClientShell_control_configure 
    extends ClientShell_resource_configure
{
    private static final int[] PARAM_VALID_RESOURCE = {
        ClientShell_resource.PARAM_SERVER,
        ClientShell_resource.PARAM_SERVICE,
    };

    public ClientShell_control_configure(ClientShell shell){
        super(shell);
    }

    public String getPluginType(){
        return ProductPlugin.TYPE_CONTROL;
    }
    
    public String getSyntaxArgs(){
        return "<" + ClientShell_resource.generateArgList(PARAM_VALID_RESOURCE) +
            "> <name>";
    }

    public String getUsageHelp(String[] args) {
        return "    " + this.getUsageShort() + ".";
    }
}
