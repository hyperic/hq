package org.hyperic.hq.plugin.exchange;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.PumpStreamHandler;

public class ExchangeUtils {
    
    protected static final String EXCHANGE_ROLE_REG_KEY = "EXCHANGE_ROLE_REG_KEY";
    protected static final String DAG_DISCOVERY = "DAG_DISCOVERY";
    protected static final String DAG_NAME = "DAG_name";
    protected static final String SITE_DISCOVERY = "SITE_DISCOVERY";
    protected static final String AD_SITE_PROP = "active_directory.site";

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
    
    protected static String fetchActiveDirectorySiteName() {
        String resultSiteName="";
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            Execute exec = new Execute(new PumpStreamHandler(output));
            exec.setCommandline(
                    new String[]{"nltest", "/DsGetSite"});
            int rc = exec.execute();
            String out = output.toString().trim();
            if (log.isDebugEnabled()) {
                log.debug("output of "+exec.getCommandLineString()+" : " + out);
            }
            if (rc == 0) { 
                BufferedReader in = null;
                in = new BufferedReader(new StringReader(output.toString()));
                resultSiteName=in.readLine();
            }
        } catch (Exception e) {
            log.warn("Failed to fetch active directory site name: " + e.getMessage(), e);
        }
        return resultSiteName;
    }
}
