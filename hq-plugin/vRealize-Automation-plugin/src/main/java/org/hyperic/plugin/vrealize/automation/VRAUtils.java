/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.plugin.vrealize.automation;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.plugin.vrealize.automation.model.cluster.config.ClusterConfig;
import org.hyperic.plugin.vrealize.automation.model.components.ComponentsRegistry;
import org.hyperic.plugin.vrealize.automation.model.components.Content;
import org.hyperic.sigar.win32.RegistryKey;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.w3c.dom.Document;

import com.vmware.hyperic.model.relations.ObjectFactory;
import com.vmware.hyperic.model.relations.RelationType;
import com.vmware.hyperic.model.relations.Resource;

/**
 * @author glaullon
 */
public class VRAUtils {

    private static final Log log = LogFactory.getLog(VRAUtils.class);
    private static final HashMap<String, Properties> propertiesMap = new HashMap<String, Properties>();

    private static final String IPv4_ADDRESS_PATTERN = "[0-9]+.[0-9]+.[0-9]+.[0-9]+";
    private static final String IPv6_ADDRESS_PATTERN =
                "([0-9a-f]+)\\:([0-9a-f]+)\\:([0-9a-f]+)\\:([0-9a-f]+)\\:([0-9a-f]+)\\:([0-9a-f]+)\\:([0-9a-f]+)\\:([0-9a-f]+)";
    private static final String VERSION_PATTERN = "[0-9].[0-9,.-]+";
    private static final int DEFAULT_TIMEOUT = 60;

    private static String localFqdn;

    public static VraVersion getVraVersion(boolean isWindows) {
        VraVersion version;
        if (isWindows) {
            version = getVraVersionWindows();

        } else {
            version = getVraVersionLinux();

        }
        return version;
    }

    public static VraVersion getVraVersionLinux() {
        String versionString = "6.1";

        String[] findVersionCommand = new String[] { "rpm", "-qa" };
        String allRunningPrograms = new String();
        try {
            allRunningPrograms = runCommandLine(findVersionCommand);
        } catch (PluginException e) {
            e.printStackTrace();
        }
        String[] runningPrograms = allRunningPrograms.split("\n");
        Pattern p = Pattern.compile("vcac-[0-9]+");
        for (String program : runningPrograms) {
            if (p.matcher(program).find()) {
                versionString = program;
                break;
            }

        }
        return extractVersionFromString(versionString);
    }

    public static VraVersion getVraVersionWindows() {
        try {
            RegistryKey vCACProgram = RegistryKey.LocalMachine.openSubKey(
                        "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\{A8DF9A88-CC1D-4FAF-B1DE-B09ABAA13540}");
            log.debug("[findVraVersionInWindows] We have the registry of: " + vCACProgram.getStringValue(
                        "DisplayName"));
            String programVersion = vCACProgram.getStringValue("DisplayVersion");
            programVersion = programVersion.trim();
            log.debug("[findVraVersionInWindows] The pogram version is: " + programVersion);
            return extractVersionFromString(programVersion);
        } catch (Win32Exception ex) {
            log.debug("[getServerResources] Error accessing to windows registry to get version. ", ex);
        }
        return null;
    }

    public static String getVcoConfFile(boolean isWindows) {
        if (isWindows) {
            String vcoInstallPathWindows = getVcoInstallPathWindows();
            File vmoProperties = new File(vcoInstallPathWindows, "\\app-server\\conf\\vmo.properties");
            try {
                return vmoProperties.getCanonicalPath();
            } catch (IOException e) {
                log.debug(String.format("Failed to read: '%s',",
                            vcoInstallPathWindows + "\\app-server\\conf\\vmo.properties"), e);
                return null;
            }
        }

        return "/etc/vco/app-server/vmo.properties";
    }

    public static String getVcoInstallPathWindows() {
        try {
            RegistryKey vCACProgram = RegistryKey.LocalMachine.openSubKey(
                        "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\vCenter Orchestrator");
            String programVersion = vCACProgram.getStringValue("InstallLocation");
            log.debug(String.format("[getVcoInstallPathWindows] installPath: '%s'", programVersion));
            return programVersion.trim();
        } catch (Win32Exception ex) {
            log.debug("[getVcoInstallPathWindows] Error accessing to windows registry to get version. ", ex);
            return null;
        }
    }

    private static VraVersion extractVersionFromString(String programVersion) {
        // TODO: change the implementation to use REGEX for splitting version onto tokens
        String versionPrefix = "vcac-";
        if (programVersion.startsWith(versionPrefix)) {
            programVersion = programVersion.substring(versionPrefix.length());
        }
        if (Pattern.matches(VERSION_PATTERN, programVersion)) {
            programVersion = programVersion.substring(0, 3);
        }

        VraVersion vraVersion = new VraVersion(6, 1);

        if (StringUtils.isNotBlank(programVersion)) {
            String[] tokens = programVersion.split("\\.");
            if (tokens.length >= 2) {
                vraVersion = new VraVersion(Integer.valueOf(tokens[0]).intValue(),
                            Integer.valueOf(tokens[1]).intValue());
            }
        }

        log.debug("[extractVersionFromString] The extracted log version is: '" + programVersion + "'");
        log.debug("[extractVersionFromString] The extracted log version is: '" + vraVersion + "'");

        return vraVersion;
    }

    public static String executeXMLQuery(
                String xmlPath, String configFilePath) {
        File configFile = new File(configFilePath);
        return executeXMLQuery(xmlPath, configFile);
    }

    public static String executeXMLQuery(
                String xPath, File xmlFile) {
        InputStream inputStream;
        String result = null;
        try {
            inputStream = new FileInputStream(xmlFile);
            result = executeXMLQuery(xPath, inputStream);
        } catch (FileNotFoundException e) {
            log.error(String.format("Unable to load configuration file [%s]", xmlFile.getName()), e);
        }
        return result;
    }

    public static String executeXMLQuery(
                String xPath, InputStream is) {
        String res = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = (Document) builder.parse(is);

            XPathFactory xFactory = XPathFactory.newInstance();
            XPath xpath = xFactory.newXPath();

            res = xpath.evaluate(xPath, doc);
        } catch (Exception ex) {
            log.error("[executeXMLQuery] " + ex, ex);
        }
        return res;
    }

    protected static Properties configFile(String filePath) {
        if (propertiesMap.containsKey(filePath))
            return propertiesMap.get(filePath);

        Properties properties = new Properties();
        // TODO: German, to implement same for Windows OS
        File configFile = new File(filePath);
        if (configFile.exists()) {
            FileInputStream in = null;
            try {
                in = new FileInputStream(configFile);
                properties.load(in);
                propertiesMap.put(filePath, properties);
            } catch (FileNotFoundException ex) {
                log.debug(ex, ex);
            } catch (IOException ex) {
                log.debug(ex, ex);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ex) {
                        log.debug(ex, ex);
                    }
                }
            }
        }

        return properties;
    }

    protected static String marshallResource(Resource model) {
        ObjectFactory factory = new ObjectFactory();
        ByteArrayOutputStream fos = new ByteArrayOutputStream();
        factory.saveModel(model, fos);
        log.debug("[marshallResource] fos=" + fos.toString());
        return fos.toString();
    }

    public static void setModelProperty(
                ServerResource server, String model) {
        server.getProductConfig().setValue(VraConstants.PROP_EXTENDED_REL_MODEL,
                    new String(Base64.encodeBase64(model.getBytes())));

        server.setProductConfig(server.getProductConfig());
    }

    public static String getFqdn(
                String containsAddress, AddressExtractor addressExtractor) {
        return getFqdn(addressExtractor.extractAddress(containsAddress));
    }

    public static String getFqdn(String address) {
        String parsedAddress = parseAddress(address);
        if (StringUtils.isBlank(parsedAddress) || StringUtils.containsIgnoreCase(parsedAddress, "localhost")) {
            if (StringUtils.isNotBlank(getLocalFqdn())) {
                return getLocalFqdn();
            }
        }

        return parsedAddress;
    }

    private static String parseAddress(String address) {
        if (StringUtils.isBlank(address)) {
            return StringUtils.EMPTY;
        }

        address = address.replace("\\:", ":");
        String fqdnOrIpFromURI = getFQDNFromURI(address);
        if (StringUtils.isNotBlank(fqdnOrIpFromURI)) {
            String fqdn = getFqdnFromIp(fqdnOrIpFromURI);
            if (StringUtils.isNotBlank(fqdn)) {
                return fqdn;
            }
            return fqdnOrIpFromURI;
        }

        address = getAddressWithoutPort(address);
        String fqdnFromIp = getFqdnFromIp(address);
        if (StringUtils.isNotBlank(fqdnFromIp)) {
            return fqdnFromIp;
        }

        return address;
    }

    private static String getAddressWithoutPort(String address) {
        int index = address.split(":").length;
        if (index > 6) {
            address = address.substring(0, address.lastIndexOf(":"));
        } else if (index == 2) {
            address = address.split(":")[0];
        }
        return address;
    }

    private static String getFqdnFromIp(String address) {
        InetAddress addr = null;
        try {
            if (Pattern.matches(IPv4_ADDRESS_PATTERN, address) || Pattern.matches(IPv6_ADDRESS_PATTERN, address)) {

                addr = InetAddress.getByName(address);
                return addr.getCanonicalHostName();
            }
        } catch (Exception e) {
        }

        return null;
    }

    private static String getFQDNFromURI(String address) {
        try {
            URI uri = new URI(address);
            String fqdn = uri.getHost();

            return fqdn;

        } catch (Exception e) {
        }
        return null;
    }

    public static Collection<String> getDnsNames(final String url) {
        Collection<String> dnsNames = null;
        try {
            DnsNameExtractor dnsExtractor = new DnsNameExtractor();
            dnsNames = dnsExtractor.getDnsNames(url);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            dnsNames = new HashSet<String>();
        }
        return dnsNames;
    }

    public static String getWGet(String path) {
        log.debug("[getWGet] Accessing the path: " + path);
        String retValue = null;
        try {
            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(
                            X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(
                            X509Certificate[] certs, String authType) {
                }
            } };
            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                public boolean verify(
                            String hostname, SSLSession session) {
                    return true;
                }
            };
            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            URL url = new URL(path);
            URLConnection con;
            try {
                con = url.openConnection();
            } catch (Exception e) {
                log.debug("[getWGet] Couldnt connect to vRa API");
                return "";
            }

            Reader reader = new InputStreamReader(con.getInputStream());
            while (true) {
                int ch = reader.read();
                if (ch == -1) {
                    break;
                }
                retValue += (char) ch;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        log.debug("[getWGet] The answer from the wget command is: " + retValue);
        return retValue;
    }

    public static void setLocalFqdn(String localFqdn) {
        VRAUtils.localFqdn = localFqdn;
    }

    public static String getLocalFqdn() {
        if (StringUtils.isBlank(localFqdn)) {
            try {
                localFqdn = InetAddress.getLocalHost().getCanonicalHostName();
            } catch (Exception e) {
                log.warn("Failed to get local FQDN", e);
            }
        }

        return localFqdn;
    }

    public static String readFile(String filePath) {
        Scanner scanner = null;
        StringBuilder result = new StringBuilder();
        try {
            result = new StringBuilder();
            scanner = new Scanner(new FileInputStream(filePath));

            while (scanner.hasNextLine()) {
                result.append(String.format("%s%n", scanner.nextLine()));
            }
        } catch (Exception e) {
            return null;
        } finally {
            if (scanner != null) {
                scanner.close();
            }
        }
        return (result == null) ? null : result.toString();
    }

    public static RelationType getDataBaseRalationType(String databaseServerFqdn) {
        if (StringUtils.equalsIgnoreCase(localFqdn, databaseServerFqdn)) {
            return RelationType.SIBLING;
        }
        return RelationType.CHILD;
    }

    public static String runCommandLine(String[] command)
                throws PluginException {
        return runCommandLine(command, DEFAULT_TIMEOUT);
    }

    public static String runCommandLine(String[] command, int timeoutSeconds)
                throws PluginException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        final PumpStreamHandler pumpStreamHandler = new PumpStreamHandler(output);
        ExecuteWatchdog wdog = new ExecuteWatchdog(timeoutSeconds * 1000);
        Execute exec = new Execute(pumpStreamHandler, wdog);
        exec.setCommandline(command);
        log.debug("[runCommand] Running the command: " + exec.getCommandLineString());
        try {
            exec.execute();
        } catch (Exception e) {
            throw new PluginException("Fail to run command: " + e.getMessage(), e);
        }
        String out = output.toString().trim();
        log.debug("[runCommand] The output is: {starting out}" + out + "'{finishing out}");
        log.debug("[runCommand] ExitValue: '" + exec.getExitValue() + "'");
        if (exec.getExitValue() != 0) {
            throw new PluginException(out);
        }
        return out;
    }

    public static Collection<String> getFqdnFromComponentRegistryJson(
                String componentsRegistryJson, String serviceName)
                throws IOException {
        Collection<String> result = new ArrayList<String>();
        if (StringUtils.isNotBlank(componentsRegistryJson)) {
            if (componentsRegistryJson.startsWith("null{")) {
                // remove the "null" prefix if present
                componentsRegistryJson = componentsRegistryJson.substring("null".length());
            }

            log.debug("[getFqdnFromComponentRegistryJson] Content: \n\n" + componentsRegistryJson);

            ObjectMapper mapper = new ObjectMapper();
            ComponentsRegistry compReg = mapper.readValue(componentsRegistryJson, ComponentsRegistry.class);

            if (compReg != null) {
                for (Content c : compReg.getContent()) {
                    if (c.getServiceName().equalsIgnoreCase(serviceName)) {
                        result.add(getFqdn(c.getStatusEndPointUrl()));
                    }
                }
            }
        } else {
            log.warn("[getFqdnFromComponentRegistryJson] JSON is EMPTY or NULL");
        }

        return result;

    }

    public static Collection<String> getNodeHostUrlsFromClusterConfigJson(String clusterConfigJson, String nodeType)
                throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ClusterConfig[] clusterConfig = mapper.readValue(clusterConfigJson, ClusterConfig[].class);

        Collection<String> result = new ArrayList<String>();
        for (int i = 0; i < clusterConfig.length; i++) {
            ClusterConfig config = clusterConfig[i];
            if (config.getNodeType().equalsIgnoreCase(nodeType)) {
                result.add(config.getNodeHost());
            }
        }
        return result;

    }

    /**
     * Compare two given FQDNs
     *
     * @param localFqdn
     * @param remoteFqdn
     * @return true if given FQDNs are equivalent
     */
    public static boolean areFqdnsEquivalent(final String localFqdn, final String remoteFqdn) {
        boolean result = false;

        if (StringUtils.isNotBlank(localFqdn) && StringUtils.isNotBlank(remoteFqdn)) {
            if (localFqdn.equalsIgnoreCase(remoteFqdn)) {
                result = true;
            } else {
                try {
                    String localHostName = InetAddress.getByName(localFqdn).getHostName();
                    String remoteHostName = InetAddress.getByName(remoteFqdn).getHostName();
                    if (localHostName.equalsIgnoreCase(remoteHostName)) {
                        result = true;
                    }
                } catch (UnknownHostException e) {
                    log.error(String.format(
                                "Unable to resolve host name for one of the given addresses.\nLocal: %s\nRemote: %s",
                                localFqdn, remoteFqdn), e);
                }
            }
        }

        return result;
    }

    public static String getJsonFromVcacConfigListCommand() {
        String resultJson = "";
        final String[] commandToGetJsonWithIaas =
                    new String[] { "/usr/sbin/vcac-config", "-v", "cluster-config", "-list" };
        String includingJsonWithIaas = new String();
        try {
            includingJsonWithIaas = VRAUtils.runCommandLine(commandToGetJsonWithIaas);
        } catch (PluginException ex) {
            log.error("[getIaaSFqdns] " + ex, ex);
        }

        if (StringUtils.isNotBlank(includingJsonWithIaas)){
            /*
             * The string is of the following format:
             *
             * ---BEGIN---
             * [{"nodeId":"cafe.node.457745596.6313"...."}]
             * ---END---
             *
             */
            String [] tokens = includingJsonWithIaas.split("\n");
            if (tokens.length >= 1){
                resultJson = tokens[1];
            }
        }


        return resultJson;
    }

    static class VraVersion {
        int major;
        int minor;
        int buildNumber;

        public VraVersion(
                    int major, int minor, int buildNumber) {
            super();
            this.major = major;
            this.minor = minor;
            this.buildNumber = buildNumber;
        }

        public VraVersion(
                    int major, int minor) {
            this(major, minor, 0);
        }

        public int getMajor() {
            return major;
        }

        public void setMajor(int major) {
            this.major = major;
        }

        public int getMinor() {
            return minor;
        }

        public void setMinor(int minor) {
            this.minor = minor;
        }

        public int getBuildNumber() {
            return buildNumber;
        }

        public void setBuildNumber(int buildNumber) {
            this.buildNumber = buildNumber;
        }

        @Override
        public String toString() {
            return "VraVersion [major=" + major + ", minor=" + minor + ", buildNumber=" + buildNumber + "]";
        }

    }

}
