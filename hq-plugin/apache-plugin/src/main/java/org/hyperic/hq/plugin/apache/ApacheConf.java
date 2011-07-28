/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.apache;

import edu.emory.mathcs.backport.java.util.Arrays;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.product.PluginException;

/**
 *
 * @author administrator
 */
public class ApacheConf {

    private String config;
    private static final Pattern virtualHostPatter = Pattern.compile("<VirtualHost ([^>]*)>([^<]*)</VirtualHost>");
    private static final Pattern serverNamePatter = Pattern.compile("[^\\S]?ServerName (.*)");
    private static final Pattern serverRootPatter = Pattern.compile("[^\\S]?ServerRoot \"?([^\\s|\"]*)\"?");
    private static final Pattern includePatter = Pattern.compile("#?[^\\S]?Include (.*)");
    private static final Pattern ipPattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
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

    public Map<String,ApacheVHost> getVHosts() throws PluginException, IOException {
        Map<String,ApacheVHost> vHost = new HashMap<String, ApacheVHost>();

        Matcher mach = virtualHostPatter.matcher(config);
        while (mach.find()) {
            String addrs = mach.group(1);
            String serverName = getServerName(mach.group(2).trim());
            List<String> addrsList = Arrays.asList(addrs.split(" "));
            for (String listen : addrsList) {
                ApacheListen l = parseListen(listen);
                ApacheVHost vh = new ApacheVHost(l.getName(), l.getIp(), l.getPort(), serverName);
                log.debug("[getVHosts] vHost=" + vh + " (" + l + ")");
                vHost.put(vh.getServerName(),vh);
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
            if (!mach.group().startsWith("#")) {
                File cf = new File(serverRoot, mach.group(1));
                if (log.isDebugEnabled()) {
                    log.debug("[replaceIncludes] ->" + mach.group());
                    log.debug("[replaceIncludes] ->" + cf + " (" + (cf.exists() && cf.isFile()) + ")");
                }
                if (cf.exists() && cf.isFile()) {
                    String includeConfig = readFile(cf);
                    String replace = replaceIncludes(includeConfig, serverRoot);
                    mach.appendReplacement(newConf, replace);
                }
            }
        }
        mach.appendTail(newConf);
        return newConf.toString();
    }

    private static String readFile(File file) throws IOException {
        final char[] buffer = new char[0x10000];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(new FileInputStream(file), "UTF-8");
        int read;
        do {
            read = in.read(buffer, 0, buffer.length);
            if (read > 0) {
                out.append(buffer, 0, read);
            }
        } while (read >= 0);
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
