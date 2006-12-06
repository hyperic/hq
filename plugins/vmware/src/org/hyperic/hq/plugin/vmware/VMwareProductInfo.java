package org.hyperic.hq.plugin.vmware;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hyperic.sigar.vmware.VM;
import org.hyperic.sigar.vmware.VMwareException;
import org.hyperic.sigar.vmware.VMwareServer;

public class VMwareProductInfo {
    private static Map cache = new HashMap();
    public String name;
    public String version;
    public String release;
    public int build;
    public int major;
    public int minor;
    public int rev;

    public String toString() {
        return "VMware " + this.name + " " +
               this.major + ".x";
    }

    public static VMwareProductInfo getInfo(Properties props)
        throws VMwareException {

        VMwareProductInfo info = (VMwareProductInfo)cache.get(props);
        if (info == null) {
            synchronized (VMwareConnectParams.LOCK) {
                info = getProductInfo(props);
            }
            cache.put(props, info);
        }

        return info;
    }

    private static VMwareProductInfo getProductInfo(Properties props)
        throws VMwareException {

        VMwareProductInfo info = null;
        VMwareConnectParams params =
            new VMwareConnectParams(props);
        VMwareServer server = new VMwareServer();
        server.connect(params);

        List names = server.getRegisteredVmNames();

        if (names.size() != 0) {
            String name = (String)names.get(0);
            VM vm = new VM();
            vm.connect(params, name);

            info = new VMwareProductInfo();

            info.name =
                VM.PRODUCTS[vm.getProductInfo(VM.PRODINFO_PRODUCT)];

            info.major = 
                vm.getProductInfo(VM.PRODINFO_VERSION_MAJOR);

            info.minor = 
                vm.getProductInfo(VM.PRODINFO_VERSION_MINOR);

            info.rev = 
                vm.getProductInfo(VM.PRODINFO_VERSION_REVISION);

            info.build =
                vm.getProductInfo(VM.PRODINFO_BUILD);

            info.version = info.major + "." + info.minor;

            info.release = info.version + "." + info.rev;

            vm.disconnect();
            vm.dispose();
        }

        server.disconnect();
        server.dispose();
        params.dispose();

        return info;
    }
}
