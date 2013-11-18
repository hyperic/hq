package org.hyperic.hq.plugin.exchange;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.AutoServerDetector;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.sigar.win32.Service;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.hq.product.ServiceResource;

import edu.emory.mathcs.backport.java.util.Collections;

public class ExchangeClientAccessDetector  extends ServerDetector
implements AutoServerDetector {
    private static final Log log =
            LogFactory.getLog(ExchangeClientAccessDetector.class.getName());


    
    private final static String IMAP4_NAME = "IMAP4";
    private final static String POP3_NAME = "POP3";
    private final static String ADDRESS_BOOK_NAME = "AddressBook";    
    private static final String[] SERVICES = {
                IMAP4_NAME,
                POP3_NAME,
                ADDRESS_BOOK_NAME
            };
    private static final Map<String, String> servicesMap = new HashMap<String, String>();
    static {        
        servicesMap.put(IMAP4_NAME,  ExchangeDetector.EX + IMAP4_NAME);
        servicesMap.put(POP3_NAME,  ExchangeDetector.EX + POP3_NAME);
        servicesMap.put(ADDRESS_BOOK_NAME,  ExchangeDetector.EX + "AB");
    }
    

    
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List<ServerResource> servers = new ArrayList<ServerResource>();
        String exe;
        Service exch = null;
        String serviceName="";
        try {
            // nira rpc client does not exist for exchange 2007 - need to check (maybe a different server)
            serviceName = getTypeProperty("SERVER_NAME");
            exch = new Service(serviceName);
            if (log.isDebugEnabled()) {
                log.debug("clientAccessDetetctor: getServerResources: looking for service:" + serviceName);
            }
            if (exch.getStatus() != Service.SERVICE_RUNNING) {
                if (log.isDebugEnabled()) {
                    log.debug("[getServerResources] service '" + serviceName
                            + "' is not RUNNING (status='" + exch.getStatusString() + "')");
                }
                
                return null;
            }
            exe = exch.getConfig().getExe().trim();
        } catch (Win32Exception e) {
            log.debug("[getServerResources] Error getting '" + serviceName
                    + "' service information " + e, e);
            return null;
        } finally {
            if (exch != null) {
                exch.close();
            }
        }

        File bin = new File(exe).getParentFile();
        if (!isInstallTypeVersion(bin.getPath())) {
            if (log.isDebugEnabled()) {
                log.debug("[getServerResources] exchange on '" + bin
                        + "' IS NOT a " + getTypeInfo().getName());
            }
            return null;
        } else {
            if (log.isDebugEnabled()) {
                log.debug("[getServerResources] exchange on '" + bin
                        + "' IS a " + getTypeInfo().getName());
            }
            
        }
        
        ConfigResponse cprops = new ConfigResponse();
        ConfigResponse productProps = new ConfigResponse();
        String roleRegKeyStr = getTypeProperty(ExchangeUtils.EXCHANGE_ROLE_REG_KEY);
        if (roleRegKeyStr != null) {
            if (!ExchangeUtils.checkRoleConfiguredAndSetVersion(roleRegKeyStr, cprops)) {
                if (log.isDebugEnabled()) {
                    log.debug("role configured  - but not found in registry - ignoring server:" + roleRegKeyStr);
                }             
                return null;
            }
            String discoverSite = getTypeProperty(ExchangeUtils.SITE_DISCOVERY);
            if (discoverSite!=null){
                String adSiteName = ExchangeUtils.fetchActiveDirectorySiteName();
                if (adSiteName!=null){
                    productProps.setValue(ExchangeUtils.AD_SITE_PROP, adSiteName);
                }
            }           
        }
        
        ServerResource server = createServerResource(exe);
        setCustomProperties(server, cprops);
        server.setIdentifier(serviceName);
        setProductConfig(server,productProps);
        server.setMeasurementConfig();
        servers.add(server);
        return servers;

    }
    
    private boolean isExchangeServiceRunning(String serviceName) {        
        return  isWin32ServiceRunning(serviceName); 
    }
    
    private ServiceResource createService(String name, String win32Name) {

        ConfigResponse cfg = new ConfigResponse();
        cfg.setValue("service_name", win32Name);

        ServiceResource service = new ServiceResource();
        service.setType(this, name);
        service.setServiceName(name);
        setProductConfig(service, new ConfigResponse());
        setMeasurementConfig(service, new ConfigResponse());
        setControlConfig(service, cfg);
        log.debug("=" + win32Name + "=> " + service.getProductConfig());
        return service;
    }
    
    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {

        List<ServiceResource> actualServices = new ArrayList<ServiceResource>();
        
        // POP3 + IMAP4 are disabled by default, only report the services
        // if they are enabled and running.        
        for(Entry<String,String> entry: servicesMap.entrySet()) {
            if(!isExchangeServiceRunning(entry.getValue())) {
                if (log.isDebugEnabled()) {
                    log.debug(entry.getValue() + " is not running");
                }                
                continue;
            }else {
                if (log.isDebugEnabled()) {
                    log.debug(entry.getValue() + " is running, adding to inventory");
                }        
            }
            actualServices.add(createService(entry.getKey(), entry.getValue()));
        }
        return actualServices;
    }


}
