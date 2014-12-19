/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import java.io.File;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.ServerResource;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.executeXMLQuery;
import static org.hyperic.plugin.vrealize.automation.VRAUtils.getFQDNFromURI;

/**
 *
 * @author glaullon
 */
public class DiscoveryVRAManagerServer extends Discovery {

    private static final Log log = LogFactory.getLog(DiscoveryVRAManagerServer.class);

    @Override
    protected ServerResource newServerResource(long pid, String exe) {
        ServerResource server = super.newServerResource(pid, exe);
        log.debug("[newServerResource] pid=" + pid);

        File configFile = new File(exe + ".config");
        log.debug("[newServerResource] configFile=" + configFile);

        String vRALB = executeXMLQuery("//serviceConfiguration/@authorizationStore", configFile);
        if (!StringUtils.isEmpty(vRALB)) {
            vRALB = getFQDNFromURI(vRALB);
        }
        log.debug("[newServerResource] vRALB (authorizationStore) = '" + vRALB + "'");

        String bdconnInfo = executeXMLQuery("//serviceConfiguration/@connectionString", configFile);
        log.debug("[newServerResource] bdConn (connectionString) = '" + bdconnInfo + "'");
        String dbFQDN = null;
        if (!StringUtils.isEmpty(bdconnInfo)) {
            String p = "Data Source=";
            int i = bdconnInfo.indexOf(p) + p.length();
            int f = bdconnInfo.indexOf(";", i);
            if ((i > -1) && (f > -1)) {
                dbFQDN = bdconnInfo.substring(i, f).trim();
            }
        }
        log.debug("[newServerResource] dbFQDN (Data Source) = '" + dbFQDN + "'");

        //TODO: relations with vRALB and dbFQDN

        return server;
    }
}
