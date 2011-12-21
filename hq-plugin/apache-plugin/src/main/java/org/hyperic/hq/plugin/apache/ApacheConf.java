package org.hyperic.hq.plugin.apache;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.regex.PatternSyntaxException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;

/**
 *
 * @author administrator
 */
public class ApacheConf {

    private String config;
    private static final Pattern virtualHostPatter = Pattern.compile("<VirtualHost ([^>]*)>", Pattern.CASE_INSENSITIVE);
    private static final Pattern serverNamePatter = Pattern.compile("ServerName (.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern serverRootPatter = Pattern.compile("ServerRoot \"?([^\\s|\"]*)\"?", Pattern.CASE_INSENSITIVE);
    private static final Pattern includePatter = Pattern.compile("Include (.*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ipPattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}", Pattern.CASE_INSENSITIVE);
    private static final Log log = LogFactory.getLog(ApacheConf.class);

    public ApacheConf(File configFile) throws PluginException {
        try {
            config = readFile(configFile);
            File serverRoot = findServerRoot(config);
            config = replaceIncludes(config, serverRoot);
        } catch (IOException ex) {
            throw new PluginException(ex.getMessage(), ex);
        }
    }

    public Map<String, ApacheVHost> getVHosts() throws PluginException, IOException {
        Map<String, ApacheVHost> vHosts = new HashMap<String, ApacheVHost>();
        int idx = 0;
        while ((idx = config.indexOf("<VirtualHost", idx)) > 0) {
            int idxEnd = config.indexOf("</VirtualHost>", idx);
            vHosts.putAll(parseVirtualHost(config.substring(idx, idxEnd)));
            idx=idxEnd;
        }
        return vHosts;
    }

    public static Map<String, ApacheVHost> parseVirtualHost(String vhTag) {
        Map<String, ApacheVHost> vHost = new HashMap<String, ApacheVHost>();
        Matcher mach = virtualHostPatter.matcher(vhTag);
        if (mach.find()) {
            String addrs = mach.group(1);
            String serverName = getServerName(vhTag);
            List<String> addrsList = Arrays.asList(addrs.split(" "));
            for (String listen : addrsList) {
                ApacheListen l = parseListen(listen);
                ApacheVHost vh = new ApacheVHost(l.getName(), l.getIp(), l.getPort(), serverName);
                log.debug("[getVHosts] vHost=" + vh + " (" + l + ")");
                vHost.put(vh.getServerName(), vh);
            }
        }
        return vHost;
    }

    public static ApacheListen parseListen(String l) {
        String ip = null, name = null, port = null;

        if (l.contains(":")) {
            String comp[] = l.split(":");
            String ipName = comp[0];
            port = comp[1];

            Matcher m = ipPattern.matcher(ipName);
            if (m.matches()) {
                ip = ipName;
                name = getHostAddress(ipName);
            } else {
                ip = getAddress(ipName);
                name = ipName;
            }
        } else {
            port = l;
        }

        ApacheListen listen = new ApacheListen(name, ip, port);
        return listen;
    }

    private static File findServerRoot(String config) throws PluginException {
        File sr = null;
        Matcher match = serverRootPatter.matcher(config);
        while (match.find() && sr == null) {
            log.debug("[findServerRoot] " + match.group());
            sr = new File(match.group(1));
            if (!(sr.exists() && sr.isDirectory())) {
                sr = null;
            }
        }

        if (sr == null) {
            throw new PluginException("Direcive 'ServerRoot' not found");
        }

        return sr;
    }

    private static String getServerName(String txt) {
        String serverName = "";
        Matcher m = serverNamePatter.matcher(txt);
        while (m.find()) {
            serverName = m.group(1).trim();
        }
        return serverName;
    }

    private static String replaceIncludes(String config, File serverRoot) throws IOException {
        Matcher mach = includePatter.matcher(config);
        StringBuffer newConf = new StringBuffer();

        while (mach.find()) {
            log.debug("[replaceIncludes] found:'" + mach.group() + "'");
            String found = mach.group(1);
            String pattern = null;
            if (isFnmatch(found)) {
                int idx = found.lastIndexOf("/");
                pattern = found.substring(idx + 1);
                found = found.substring(0, idx);
            }

            File cf = new File(found);
            if (!cf.isAbsolute()) {
                cf = new File(serverRoot, found);
            }

            if (cf.exists()) {
                if (cf.isFile()) {
                    log.debug("[replaceIncludes] Including file: '" + cf + "'");
                    String includeConfig = readFile(cf);
                    includeConfig = Matcher.quoteReplacement(includeConfig);
                    String replace = replaceIncludes(includeConfig, serverRoot);
                    mach.appendReplacement(newConf, replace);
                }
                if (cf.isDirectory()) {
                    List<File> files = listFiles(cf);
                    log.debug("[replaceIncludes] Browsing directory: '" + cf + "' (" + files.size() + " files)");
                    StringBuilder replace = new StringBuilder();
                    for (File file : files) {
                        if ((pattern == null) || fnmatch(pattern, file.getName())) {
                            log.debug("[replaceIncludes] Including file: '" + file + "'");
                            String includeConfig = readFile(file);
                            includeConfig = Matcher.quoteReplacement(includeConfig);
                            replace.append(replaceIncludes(includeConfig, serverRoot));
                        } else {
                            log.debug("[replaceIncludes] Ignoring file:  '" + file + "' pattern='" + pattern + "'");
                        }
                    }
                    mach.appendReplacement(newConf, replace.toString());
                }
            } else {
                log.debug("[replaceIncludes] File Not Found!! '" + cf + "'");
            }
        }
        mach.appendTail(newConf);
        return newConf.toString();
    }

    private static List<File> listFiles(File dir) {
        List<File> files = new ArrayList<File>();
        File ls[] = dir.listFiles();
        for (File file : ls) {
            if (file.isDirectory()) {
                files.addAll(listFiles(file));
            } else {
                files.add(file);
            }
        }
        return files;
    }

    private static boolean isFnmatch(String pattern) {
        if (pattern.contains("*") || pattern.contains("*")) {
            return true;
        } else if (pattern.contains("[") || pattern.contains("]")) {
            return true;
        } else if (pattern.contains("\\") && !pattern.endsWith("\\")) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean fnmatch(String pattern, String filename) {
        if (filename.startsWith(".") && !pattern.startsWith(".")) {
            return false;
        }
        if (filename.contains("/.") && !pattern.contains("/.")) {
            return false;
        }

        pattern = pattern.replace("?", ".");
        pattern = pattern.replace("*", ".*");
        pattern = pattern.replaceAll("\\\\(\\w)", "$1");
        pattern = pattern.replace("[!", "[^");

        Pattern p;
        try {
            p = Pattern.compile(pattern + "$");
        } catch (PatternSyntaxException ex) {
            log.debug(ex, ex);
            return false;
        }
        Matcher m = p.matcher(filename);
        return m.matches();
    }

    private static String readFile(File file) throws IOException {
        final char[] buffer = new char[0x10000];
        StringBuilder out = new StringBuilder();
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (!line.startsWith("#") && line.length() > 0) {
                out.append(line).append('\n');
            }
        }
        return out.toString();
    }

    private static String getHostAddress(String ip) {
        try {
            return InetAddress.getByAddress(getIpAsArrayOfByte(ip)).getHostName();
        } catch (UnknownHostException ex) {
            return ex.toString();
        }
    }

    private static String getAddress(String name) {
        try {
            return getIpAsString(InetAddress.getByName(name).getAddress());
        } catch (UnknownHostException ex) {
            return ex.toString();
        }
    }

    private static String getIpAsString(byte[] rawBytes) {
        int i = 4;
        StringBuilder ipAddress = new StringBuilder();
        for (byte raw : rawBytes) {
            ipAddress.append(raw & 0xFF);
            if (--i > 0) {
                ipAddress.append(".");
            }
        }
        return ipAddress.toString();
    }

    private static byte[] getIpAsArrayOfByte(String ipAddress) {
        StringTokenizer st = new StringTokenizer(ipAddress, ".");
        byte[] ip = new byte[4];
        int i = 0;

        while (st.hasMoreTokens()) {
            ip[i++] = (byte) Integer.parseInt(st.nextToken());
        }

        return ip;
    }
}
