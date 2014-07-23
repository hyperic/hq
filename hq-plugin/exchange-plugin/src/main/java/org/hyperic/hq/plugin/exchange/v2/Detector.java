package org.hyperic.hq.plugin.exchange.v2;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.exchange.ExchangeDagDetector;
import org.hyperic.hq.plugin.exchange.ExchangeUtils;
import org.hyperic.hq.plugin.mssql.PDH;
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
    private final MSService[] services = {
        new MSService("MSExchangeADTopology", "Active Directory Topology", true),
        new MSService("MSExchangeAntispamUpdate", "Anti-spam Update", false),
        new MSService("MSExchangeDelivery", "Mailbox Transport Delivery", true),
        new MSService("MSExchangeDiagnostics", "Diagnostics", false),
        new MSService("MSExchangeEdgeSync", "EdgeSync", true),
        new MSService("MSExchangeFastSearch", "Search", false),
        new MSService("MSExchangeFrontEndTransport", "Frontend Transport", false),
        new MSService("MSExchangeHM", "Health Manager", false),
        new MSService("MSExchangeImap4", "IMAP4", false),
        new MSService("MSExchangeIMAP4BE", "IMAP4 Backend", false),
        new MSService("MSExchangeIS", "Information Store", true),
        new MSService("MSExchangeMailboxAssistants", "Mailbox Assistants", true),
        new MSService("MSExchangeMailboxReplication", "Mailbox Replication", true),
        new MSService("MSExchangeMonitoring", "Monitoring", false),
        new MSService("MSExchangePop3", "POP3", false),
        new MSService("MSExchangePOP3BE", "POP3 Backend", false),
        new MSService("MSExchangeRepl", "Replication", true),
        new MSService("MSExchangeRPC", "RPC Client Access", true),
        new MSService("MSExchangeServiceHost", "Service Host", true),
        new MSService("MSExchangeSubmission", "Mailbox Transport Submission", true),
        new MSService("MSExchangeThrottling", "Throttling", true),
        new MSService("MSExchangeTransport", "Transport", true),
        new MSService("MSExchangeTransportLogSearch", "Transport Log Search", true),
        new MSService("MSExchangeUM", "Unified Messaging", true),
        new MSService("MSExchangeUMCR", "Unified Messaging Call Router", false)
    };

    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        log.info("[getServerResources] platformConfig=" + platformConfig);
        List servers = new ArrayList();

        if (checkVersion()) {
            final String installPath = getInstallPath();
            final String platformName = getPlatformName();

            int timeout = 20;
            try {
                timeout = Integer.parseInt(getManager().getProperty("exchange.v2.ps.timeout", "30"));
            } catch (NumberFormatException e) {
                log.info("[getServerResources] error: 'exchange.v2.ps.timeout' " + e, e);
            }
            log.info("[getServerResources] exchange.v2.ps.timeout=" + timeout);

            String roles = getExchangeServerRoles(installPath, platformName, timeout);

            if (roles.trim().length() > 0) {
                ConfigResponse productProps = new ConfigResponse();
                productProps.setValue("Roles", roles);
                productProps.setValue(ExchangeUtils.AD_SITE_PROP, ExchangeUtils.fetchActiveDirectorySiteName());

                String dagName = ExchangeDagDetector.getDagName(installPath, getPlatformName(), timeout);
                if (dagName != null) {
                    productProps.setValue(ExchangeUtils.DAG_NAME, dagName);
                }

                StringBuilder sb = new StringBuilder();
                for (MSService service : services) {
                    if (service.isRequired() && isWin32ServiceRunning(service.getWindowsName())) {
                        sb.append(service.getWindowsName()).append(", ");
                    }
                }
                String sl = sb.toString().trim();
                if (sl.endsWith(",")) {
                    sl = sl.substring(0, sl.length() - 1);
                }
                productProps.setValue("services", sl);

                log.info("[getServerResources] platformName=" + platformName);
                log.info("[getServerResources] installPath=" + installPath);
                log.info("[getServerResources] productProps=" + productProps);

                ServerResource server = createServerResource(installPath);
                servers.add(server);
                setCustomProperties(server, new ConfigResponse());
                setProductConfig(server, productProps);
                setMeasurementConfig(server, new ConfigResponse());
            } else {
                log.info("[getServerResources] Roles not found, skiping this server.");
            }
        }

        return servers;
    }

    @Override
    protected List discoverServices(ConfigResponse config) throws PluginException {
        log.debug("[discoverServices] config=" + config);
        List res = new ArrayList();

        for (MSService service : services) {
            boolean runnig = isWin32ServiceRunning(service.getWindowsName());
            log.debug("[discoverServices] service:'" + service.getWindowsName() + "'(" + service.getServiceName() + ") runnig:" + (runnig ? "YES" : "NO"));

            ConfigResponse cfg = new ConfigResponse();
            cfg.setValue("service_name", service.getWindowsName());

            if (runnig) {
                ServiceResource s = new ServiceResource();
                s.setType(this, service.getServiceName());
                s.setServiceName(service.getServiceName());
                setProductConfig(s, cfg);
                setMeasurementConfig(s, new ConfigResponse());

                res.add(s);
            }
        }

        String[] addcs = PDH.getInstances("MSExchange ADAccess Domain Controllers");
        for (String addc : addcs) {
            ConfigResponse cfg = new ConfigResponse();
            cfg.setValue("instance_name", addc);
            ServiceResource s = new ServiceResource();
            s.setType(this, "ADAccess Domain Controller");
            s.setServiceName("ADAccess Domain Controller " + addc);
            setProductConfig(s, cfg);
            setMeasurementConfig(s, new ConfigResponse());

            res.add(s);
        }

        String[] dbs = PDH.getInstances("MSExchange Database");
        for (String db : dbs) {
            ConfigResponse cfg = new ConfigResponse();
            cfg.setValue("instance_name", db);
            ServiceResource s = new ServiceResource();
            s.setType(this, "Database");
            s.setServiceName("Database " + db);
            setProductConfig(s, cfg);
            setMeasurementConfig(s, new ConfigResponse());

            res.add(s);
        }

        String[] dbis = PDH.getInstances("MSExchange Database ==> Instances");
        for (String db : dbis) {
            ConfigResponse cfg = new ConfigResponse();
            cfg.setValue("instance_name", db);
            ServiceResource s = new ServiceResource();
            s.setType(this, "Database Instance");
            s.setServiceName("Database Instance " + db);
            setProductConfig(s, cfg);
            setMeasurementConfig(s, new ConfigResponse());

            res.add(s);
        }

        return res;
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
            log.debug("[checkVersion] regPathStr='" + regPathStr + "' " + e, e);
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

    public static String getExchangeServerRoles(String exchangeInstallDir, String platform, int timeout) {
        File exchangePSBase = new File(exchangeInstallDir, "bin");
        File exchangePS = new File(exchangePSBase, "RemoteExchange.ps1");
        if (!exchangePS.exists()) {
            exchangePS = new File(exchangePSBase, "Exchange.ps1"); // SP1 edge.
        }

        String exchangePSStr;
        try {
            exchangePSStr = exchangePS.getCanonicalPath();
        } catch (IOException ex) {
            exchangePSStr = exchangePS.getAbsolutePath();
        }

        String[] command = new String[]{ExchangeUtils.POWERSHELL_COMMAND, "-command",
            "\". '" + exchangePSStr + "';"
            + " Connect-ExchangeServer -auto -ClientApplication:ManagementShell ;"
            + " Get-ExchangeServer -Identity " + platform + " | format-list ServerRole \""};
        log.debug("[getExchangeServerRoles] command: " + Arrays.asList(command));

        String commandOutput = ExchangeUtils.runCommand(command, timeout);

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
        if (roles.endsWith(",")) {
            roles = roles.substring(0, roles.length() - 1);
        }

        log.debug("[getExchangeServerRoles] roles : " + roles);

        return roles;
    }

    private class MSService {

        private final String windowsName;
        private final String serviceName;
        private final boolean required;

        public MSService(String windowsName, String serviceName, boolean critacal) {
            this.windowsName = windowsName;
            this.serviceName = serviceName;
            this.required = critacal;
        }

        public String getWindowsName() {
            return windowsName;
        }

        public String getServiceName() {
            return serviceName;
        }

        public boolean isRequired() {
            return required;
        }
    }
}
