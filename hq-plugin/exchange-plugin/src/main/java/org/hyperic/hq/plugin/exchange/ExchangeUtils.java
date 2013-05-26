package org.hyperic.hq.plugin.exchange;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;

public class ExchangeUtils {
    
    protected static final String EXCHANGE_ROLE_REG_KEY = "EXCHANGE_ROLE_REG_KEY";
    private static final Log log =
            LogFactory.getLog(ExchangeUtils.class.getName());

    
    protected static boolean checkRoleConfiguredAndSetVersion(String roleRegKeyStr, ConfigResponse cprops) {        
        // check if role exists in registry
        RegistryKey  key = null;
        try {
            key = RegistryKey.LocalMachine.openSubKey(roleRegKeyStr);
            String version = key.getStringValue("ConfiguredVersion").trim();
            if (log.isDebugEnabled()) {
                log.debug("version=[" + version +"]");
            }            
            if (version != null) {
                cprops.setValue("version", version);
            }
            return true;
        }
        catch (Win32Exception e) {            
            log.warn("ExchangeUtils: didn't find ConfiguredVersion in:" + roleRegKeyStr);
            return false;
       }           
       finally {
           if (key != null) {
               key.close();
           }
       }
        
    }


}
