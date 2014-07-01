/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.exchange.v2;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.exchange.ExchangeDagDetector;
import org.hyperic.hq.plugin.exchange.ExchangeUtils;
import org.hyperic.hq.product.AutoServerDetector;
import static org.hyperic.hq.product.GenericPlugin.getPlatformName;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerDetector;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.hq.product.ServiceResource;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.config.ConfigResponse;

/**
 *
 * @author glaullon
 */
public class Detector extends ServerDetector implements AutoServerDetector {

    private static final Log log = LogFactory.getLog(Detector.class.getName());
    private final String[][] servicesNAmes = {
        {"MSExchangeADTopology", "Active Directory Topology"},
        {"MSExchangeAntispamUpdate", "Anti-spam Update"},
        {"MSExchangeDelivery", "Mailbox Transport Delivery"},
        {"MSExchangeDiagnostics", "Diagnostics"},
        {"MSExchangeEdgeSync", "EdgeSync"},
        {"MSExchangeFastSearch", "Search"},
        {"MSExchangeFrontEndTransport", "Frontend Transport"},
        {"MSExchangeHM", "Health Manager"},
        {"MSExchangeImap4", "IMAP4"},
        {"MSExchangeIMAP4BE", "IMAP4 Backend"},
        {"MSExchangeIS", "Information Store"},
        {"MSExchangeMailboxAssistants", "Mailbox Assistants"},
        {"MSExchangeMailboxReplication", "Mailbox Replication"},
        {"MSExchangeMonitoring", "Monitoring"},
        {"MSExchangePop3", "POP3"},
        {"MSExchangePOP3BE", "POP3 Backend"},
        {"MSExchangeRepl", "Replication"},
        {"MSExchangeRPC", "RPC Client Access"},
        {"MSExchangeServiceHost", "Service Host"},
        {"MSExchangeSubmission", "Mailbox Transport Submission"},
        {"MSExchangeThrottling", "Throttling"},
        {"MSExchangeTransport", "Transport"},
        {"MSExchangeTransportLogSearch", "Transport Log Search"},
        {"MSExchangeUM", "Unified Messaging"},
        {"MSExchangeUMCR", "Unified Messaging Call Router"}
    };

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List servers = new ArrayList();

        if (checkVersion()) {
            final String installPath = getInstallPath();
            final String platformName = getPlatformName();

            ConfigResponse productProps = new ConfigResponse();
            productProps.setValue("Roles", getExchangeServerRoles(installPath, platformName));
            productProps.setValue(ExchangeUtils.AD_SITE_PROP, ExchangeUtils.fetchActiveDirectorySiteName());

            String dagName = ExchangeDagDetector.getDagName(installPath, getPlatformName());
            if (dagName != null) {
                productProps.setValue(ExchangeUtils.DAG_NAME, dagName);
            }

            ServerResource server = createServerResource(installPath);
            servers.add(server);
            setCustomProperties(server, new ConfigResponse());
            setProductConfig(server, productProps);
            setMeasurementConfig(server, new ConfigResponse());
        }

        return servers;
    }

    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {
        log.debug("[discoverServices] config=" + config);
        List services = new ArrayList();

        for (String[] serviceName : servicesNAmes) {
            boolean runnig = isWin32ServiceRunning(serviceName[0]);
            log.debug("[discoverServices] service:'" + serviceName[1] + "'(" + serviceName[0] + ") runnig:" + (runnig ? "YES" : "NO"));

            ConfigResponse cfg = new ConfigResponse();
            cfg.setValue("service_name", serviceName[0]);

            if (runnig) {
                ServiceResource service = new ServiceResource();
                service.setType(this, serviceName[1]);
                service.setServiceName(serviceName[1]);
                setProductConfig(service, cfg);
                setMeasurementConfig(service, new ConfigResponse());

                services.add(service);
            }
        }

        return services;
    }

    private boolean checkVersion() {
        String regPathStr = getTypeProperty("REG_PATH");
        String regNameStr = getTypeProperty("REG_NAME");
        String regValueStr = getTypeProperty("REG_VALUE");

        RegistryKey key = null;
        boolean res = false;

        try {
            key = RegistryKey.LocalMachine.openSubKey(regPathStr);
            String version = key.getStringValue(regNameStr).trim();
            res = version.equalsIgnoreCase(regValueStr);
            if (log.isDebugEnabled()) {
                log.debug("[checkVersion] version='" + version + "' res='" + res + "'");
            }
        } catch (Win32Exception e) {
            log.warn("[checkVersion] regPathStr='" + regPathStr + "' " + e, e);
        } finally {
            if (key != null) {
                key.close();
            }
        }
        return res;
    }

    private String getInstallPath() {
        String regPathStr = getTypeProperty("REG_PATH");
        String regInstallPath = getTypeProperty("REG_NAME_INSTALL_PATH");

        RegistryKey key = null;
        String res = "";

        try {
            key = RegistryKey.LocalMachine.openSubKey(regPathStr);
            res = key.getStringValue(regInstallPath).trim();
            if (log.isDebugEnabled()) {
                log.debug("[checkVersion] installpath='" + res + "'");
            }
        } catch (Win32Exception e) {
            log.debug("[checkVersion][ERROR] regPathStr='" + regPathStr + "' regInstallPath='" + regInstallPath + "' " + e, e);
        } finally {
            if (key != null) {
                key.close();
            }
        }
        return res;
    }

    public static String getExchangeServerRoles(String exchangeInstallDir, String platform) {
        String[] command = new String[]{ExchangeUtils.POWERSHELL_COMMAND, "-command",
            "\". '"
            + exchangeInstallDir
            + "\\bin\\RemoteExchange.ps1'; Connect-ExchangeServer -auto -ClientApplication:ManagementShell ; Get-ExchangeServer -Identity " + platform + " | format-list ServerRole \""};
        log.debug("[getExchangeServerRoles] command: " + Arrays.asList(command));

        String commandOutput = ExchangeUtils.runCommand(command);

        // removing empty lines and wrap lines
        StringBuilder sb = new StringBuilder();
        String lines[] = commandOutput.replace("\r", "").split("\n");
        for (String line : lines) {
            if (line.trim().length() > 0) {
                sb.append(line).append("\n");
            }
        }
        commandOutput = sb.toString().replaceAll("\\n\\s+", "");

        log.debug("[getExchangeServerRoles] commandOutput: " + commandOutput);

        Pattern p = Pattern.compile("^([\\S]*)[\\s]*:\\s(.*)$", Pattern.MULTILINE);
        Matcher m = p.matcher(commandOutput);

        String roles = "";

        while (m.find()) {
            String k = m.group(1);
            String v = m.group(2);
            log.debug("[getExchangeServerRoles] " + k + " : " + v);
            if (k.equals("ServerRole")) {
                roles = v.trim();
            }
        }
        log.debug("[getExchangeServerRoles] roles : " + roles);

        return roles;
    }

}

// Get-ExchangeServer -Identity EXCH2K13-SA.qa1.vmware.com | format-list ServerRole
