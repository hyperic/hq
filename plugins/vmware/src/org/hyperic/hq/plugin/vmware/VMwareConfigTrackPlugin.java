package org.hyperic.hq.plugin.vmware;

import org.hyperic.hq.product.ConfigFileTrackPlugin;
import org.hyperic.hq.product.TypeInfo;

public class VMwareConfigTrackPlugin extends ConfigFileTrackPlugin {

    public String getDefaultConfigFile(String installPath) {
        //these lame defaults should never get used, they are auto-discovered
        if (getTypeInfo().getType() == TypeInfo.TYPE_SERVER) {
            return "server-config-file";
        }
        else {
            return "${vm.name}.vmx";
        }
    }
}
