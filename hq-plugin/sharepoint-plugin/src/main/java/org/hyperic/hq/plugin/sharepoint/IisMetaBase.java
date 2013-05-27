/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2013], Hyperic, Inc.
 * This file is part of HQ.
 * 
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
package org.hyperic.hq.plugin.sharepoint;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.XPathAPI;
import org.hyperic.sigar.win32.MetaBase;
import org.hyperic.sigar.win32.Win32Exception;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class IisMetaBase {

    private static final Log log =
            LogFactory.getLog(IisMetaBase.class.getName());
    private static final String IIS_MKEY = "/LM/W3SVC";
    private static final int MD_SSL_ACCESS_PERM = 6030;
    private static final int MD_ACCESS_SSL = 0x00000008;
    protected static final String APPCMD = "C:/Windows/System32/inetsrv/appcmd.exe";
    private String id;
    private String ip;
    private String hostname;
    private String port;
    private String path;
    private boolean requireSSL = false;
    private String name;

    @Override
    public String toString() {
        return "IisMetaBase{" + "id=" + id + ", name=" + name + ", ip=" + ip + ", hostname=" + hostname + ", port=" + port + ", path=" + path + ", requireSSL=" + requireSSL + '}';
    }

    public String toUrlString() throws MalformedURLException {
        URL url = new URL(requireSSL ? "https" : "http", ip, Integer.parseInt(port), "/");
        String urlStr = url.toString();
        log.debug("web: '" + this + "'");
        log.debug("urlStr: '" + urlStr + "'");
        return urlStr;
    }

    public static Map<String, IisMetaBase> getWebSites()
            throws Win32Exception {

        if (new File(APPCMD).exists()) {
            try {
                return getWebSitesViaAppCmd(); //IIS7
            } catch (Exception e) {
                log.debug(APPCMD + ": " + e, e);
                throw new Win32Exception(e.getMessage());
            }
        } else {
            return getWebSitesViaMetaBase();
        }
    }

    private static boolean parseBinding(IisMetaBase info, String entry) {
        if (entry == null) {
            return false;
        }
        int ix = entry.indexOf(":");
        if (ix == -1) {
            return false;
        }

        //binding format:
        //"listen ip:port:host header"
        info.ip = entry.substring(0, ix);

        entry = entry.substring(ix + 1);
        ix = entry.indexOf(":");
        info.port = entry.substring(0, ix);

        //if host header is defined, URLMetric
        //will add Host: header with this value.
        info.hostname = entry.substring(ix + 1);
        if ((info.hostname != null)
                && (info.hostname.length() == 0)) {
            info.hostname = null;
        }

        if ((info.ip == null)
                || (info.ip.length() == 0)
                || (info.ip.equals("*"))) {
            //not bound to a specific ip
            info.ip = "localhost";
        }

        return true;
    }

    //IIS7 does not use MetaBase
    private static Map getWebSitesViaAppCmd()
            throws Exception {

        final String[] cmd = {
            APPCMD, "list", "config",
            "-section:system.applicationHost/sites"
        };

        Map websites = new HashMap();

        ByteArrayOutputStream output =
                new ByteArrayOutputStream();

        ExecuteWatchdog wdog =
                new ExecuteWatchdog(5 * 1000);
        Execute exec =
                new Execute(new PumpStreamHandler(output), wdog);

        exec.setCommandline(cmd);

        try {
            int exitStatus = exec.execute();
            if (exitStatus != 0 || wdog.killedProcess()) {
                log.debug(Arrays.asList(cmd) + ": " + output);
                return websites;
            }
        } catch (Exception e) {
            log.debug(Arrays.asList(cmd) + ": " + e);
            return websites;
        }

        DocumentBuilderFactory dbf =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document config =
                db.parse(new ByteArrayInputStream(output.toString().getBytes("UTF-8")));

        NodeList sites =
                XPathAPI.selectNodeList(config, "//sites/site");

        for (int i = 0; i < sites.getLength(); i++) {
            Element site = (Element) sites.item(i);
            String name = site.getAttribute("name");

            IisMetaBase info = new IisMetaBase();
            info.id = site.getAttribute("id");

            String sitePath = "//site[@name=\"" + name + "\"]/";
            String bindPath = sitePath + "bindings/binding[1]";
            String docPath =
                    sitePath + "application[1]/virtualDirectory[1]/@physicalPath";

            Element binding =
                    (Element) XPathAPI.selectSingleNode(site, bindPath);

            if (binding == null) {
                log.debug("No bindings defined for: " + name);
                continue;
            }
            String proto = binding.getAttribute("protocol");
            if (proto != null) {
                if ("https".equals(proto.toString().trim())) {
                    info.requireSSL = true;
                }
            }
            String bindInfo = binding.getAttribute("bindingInformation");
            if (!parseBinding(info, bindInfo)) {
                log.debug("Failed to parse bindingInformation="
                        + bindInfo + " for: " + name);
                continue;
            }
            Object docRoot = XPathAPI.eval(site, docPath);
            if (docRoot != null) {
                info.path = docRoot.toString();
            }
            log.debug(name + "=" + info);
            websites.put(name, info);
        }

        return websites;
    }

    private static Map<String, IisMetaBase> getWebSitesViaMetaBase()
            throws Win32Exception {
        String keys[];
        Map websites = new HashMap();
        MetaBase mb = new MetaBase();

        try {
            mb.OpenSubKey(IIS_MKEY);
            keys = mb.getSubKeyNames();
        } finally {
            mb.close();
        }

        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            int id;
            if (!Character.isDigit(key.charAt(0))) {
                continue;
            }

            try {
                id = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                continue;
            }

            String subkey = IIS_MKEY + "/" + id;
            MetaBase srv = null;
            try {
                srv = new MetaBase();
                srv.OpenSubKey(subkey);

                String[] bindings = null;

                IisMetaBase info = new IisMetaBase();

                //IIS 6.0+Windows 2003 has Administration website
                //that requires SSL by default.
                //Any Web Site can be configured to required ssl.
                try {
                    int flags = srv.getIntValue(MD_SSL_ACCESS_PERM);
                    info.requireSSL = (flags & MD_ACCESS_SSL) != 0;
                    if (info.requireSSL) {
                        bindings =
                                srv.getMultiStringValue(MetaBase.MD_SECURE_BINDINGS);
                    }
                } catch (Win32Exception e) {
                }

                if (bindings == null) {
                    bindings =
                            srv.getMultiStringValue(MetaBase.MD_SERVER_BINDINGS);
                }
                info.id = key;

                if (bindings.length == 0) {
                    continue;
                }

                if (!parseBinding(info, bindings[0])) {
                    continue;
                }
                String name =
                        srv.getStringValue(MetaBase.MD_SERVER_COMMENT);
                info.setName(name);

                websites.put(name, info);

                //XXX this is bogus, else locks the metabase
                //because OpenSubKey does not close the key
                //thats already open.
                srv.close();
                srv = null;
                srv = new MetaBase();
                srv.OpenSubKey(subkey + "/ROOT");
                String docroot =
                        srv.getStringValue(3001);
                info.path = docroot;
            } catch (Win32Exception e) {
            } finally {
                if (srv != null) {
                    srv.close();
                }
            }
        }

        return websites;
    }

    public static void main(String[] args) throws Exception {
        Map websites = IisMetaBase.getWebSites();
        System.out.println(websites);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
