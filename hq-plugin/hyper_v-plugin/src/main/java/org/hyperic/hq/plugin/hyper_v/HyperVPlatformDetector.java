package org.hyperic.hq.plugin.hyper_v;

import java.io.File;

import org.hyperic.hq.product.PlatformDetector;
import org.hyperic.hq.product.PlatformResource;
import org.hyperic.hq.product.PluginException;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;

public class HyperVPlatformDetector extends PlatformDetector {
    private RegistryKey openRootKey(String key) {
        try {
            return RegistryKey.LocalMachine.openSubKey(key);
        }
        catch (Win32Exception e) {
//            _log.debug("Could not open registry key '" +
//                       key + "': " + e.getMessage());
            return null;
        }
    }

    public PlatformResource getPlatformResource(ConfigResponse config) throws PluginException {
        RegistryKey rootReg = openRootKey("SYSTEM\\CurrentControlSet\\Services\\vmms");
        try {
            String path = rootReg.getStringValue("ImagePath");
            if (!new File(path).exists()) {
//                log.debug(path + " does not exist");
                return null;
            }
            PlatformResource platform = super.getPlatformResource(config);
            return platform;
        }catch(Win32Exception e) {
            throw new PluginException(e);
        }
    }
}
