/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
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

package org.hyperic.hq.plugin.weblogic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;

/**
 * Config parsing intended only for WebLogic Admin server auto discovery.
 */
public class WeblogicConfig {

    String version;
    String domain;
    String adminPort;
    boolean adminPortEnabled;
    HashMap runningNodes = new HashMap();
    ArrayList servers = new ArrayList();

    class Server {
        String domain;
        String name;
        String version;
        String url;

        public Properties getProperties() {
            Properties props = new Properties();

            props.setProperty(WeblogicMetric.PROP_DOMAIN,
                              this.domain);

            props.setProperty(WeblogicMetric.PROP_ADMIN_URL,
                              this.url);

            props.setProperty(WeblogicMetric.PROP_SERVER,
                              this.name);

            return props;
        }

        public String getVersion() {
            if (this.version == null) {
                return WeblogicProductPlugin.VERSION_61; //lousy guess
            }

            return this.version;
        }
        
        private void setURL(String protocol,
                            String addr,
                            String port) {
            this.url = protocol + "://" + addr + ":" + port;
        }
    }

    public void read(File file)
        throws IOException {

        FileInputStream is = null;

        try {
            is = new FileInputStream(file);
            parse(is);
        } catch (SAXException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) { }
            }
        }

        if ((this.version != null) &&
             majorVersion(this.version) >= 9)
        {
            for (int i=0; i<this.servers.size(); i++) {
                Server server = (Server)this.servers.get(i);
                if (server.version == null) {
                    server.version = this.version;
                }
            }
        }

        if (!WeblogicProductPlugin.VERSION_61.equals(this.version)) {
            return;
        }

        //7.0/8.1 has a filesystem layout we can use to
        //detect the admin server.
        //6.1 does not so we try to guess.

        file = new File(file.getParentFile(),
                        "running-managed-servers.xml");

        if (!file.exists()) {
            return;
        }

        try {
            is = new FileInputStream(file);
            parseRunningServers(is);
        } finally {
            if (is != null) {
                try { is.close(); } catch (IOException e) { }
            }
        }
    }

    public Server getServer(String name) {
        for (int i=0; i<this.servers.size(); i++) {
            Server server = (Server)this.servers.get(i);
            if (server.name.equals(name)) {
                return server;
            }
        }
        return null;
    }

    public Server guessAdminServer() {
        for (int i=0; i<this.servers.size(); i++) {
            Server server = (Server)this.servers.get(i);

            if (this.runningNodes.get(server.name) != null) {
                continue;
            }

            return server;
        }

        return (Server)this.servers.get(0);
    }

    private String getAttribute(Node node, String name) {
        NamedNodeMap attrs = node.getAttributes();
        if (attrs == null) {
            return null;
        }
        Node item = attrs.getNamedItem(name);
        if (item == null) {
            return null;
        }
        return item.getNodeValue();
    }

    private String versionSubstring(String version) {
        int ix = version.indexOf('.');
        if (ix == -1){
            return version.substring(0, 3);
        }
        else {
            return version.substring(0, ix+2);
        }
    }

    public static int majorVersion(String version) {
        int ix = version.indexOf('.');
        if (ix == -1) {
            return Integer.parseInt(version);
        }
        else {
            return Integer.parseInt(version.substring(0, ix));
        }
    }

    private String getVersion(Node node, String attr) {
        String vers = getAttribute(node, attr);

        if ((vers != null) && vers.length() >= 3) {
            //7.0|8.1
            return versionSubstring(vers);
        }

        return this.version;
    }

    private void parseRunningServers(FileInputStream is)
        throws IOException {

        BufferedReader reader =
            new BufferedReader(new InputStreamReader(is));
        String line;

        //XXX somewhat ghetto.  xml parser could not resolve
        // "/weblogic/management/xml/managed-server.dtd"
        //which is in weblogic.jar, but was being treated as
        //a file url.  had tried turning off dtd validation
        //but did not work against the 6.1 jar.
        //wasted enough time on it, this file is always in the
        //same format since its generated.
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.equals("<server-name>")) {
                continue;
            }

            String name = reader.readLine();
            this.runningNodes.put(name.trim(), Boolean.TRUE);
        }
    }

    private void parse(InputStream is)
        throws IOException,
        SAXException,
        ParserConfigurationException {

        DocumentBuilderFactory dbf =
            DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(is);
        Element domain = doc.getDocumentElement();

        this.version = getVersion(domain, "ConfigurationVersion"); 
        this.domain = getAttribute(domain, "Name");
        this.adminPort = getAttribute(domain, "AdministrationPort");
        this.adminPortEnabled = 
            "true".equals(getAttribute(domain, "AdministrationPortEnabled"));

        NodeList nodes = domain.getChildNodes();

        for (int i=0; i<nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String tag = node.getNodeName();

            if (tag.equals("Server")) {
                Server server = new Server();
                server.domain = this.domain;
                server.name = getAttribute(node, "Name");
                server.version = getVersion(node, "ServerVersion");

                String port = null;
                if (this.adminPortEnabled) {
                    port = getAttribute(node, "AdministrationPort");
                    if (port == null) {
                        port = this.adminPort;
                    }
                }

                if (port == null) {
                    //config.xml in the examples have
                    //AdministrationPortEnabled=true
                    //but no AdministrationPort set if the
                    //server has never been started.
                    port = getAttribute(node, "ListenPort");
                    this.adminPortEnabled = false;
                }

                String addr = getAttribute(node, "ListenAddress");
                if ((addr == null) || //not running
                    "".equals(addr))  //weblogic81 configbuilder bug
                {
                    addr = "localhost"; 
                }

                String protocol;
                if (this.adminPortEnabled) {
                    protocol = "t3s";
                }
                else {
                    protocol = "t3";
                }
                server.setURL(protocol, addr, port);

                this.servers.add(server);
            }
            //9.0 stuff
            else if (tag.equals("server")) {
                String protocol = "t3";
                String addr="localhost", port="7001";
                Server server = new Server();
                server.domain = this.domain;
                server.version = this.version;

                NodeList srvNodes = node.getChildNodes();

                for (int j=0; j<srvNodes.getLength(); j++) {
                    Node srv = srvNodes.item(j);
                    String srvTag = srv.getNodeName();

                    if (srvTag.equals("name")) {
                        server.name = getText(srv);
                    }
                    else if (srvTag.equals("network-access-point")) {
                        NodeList netNodes = srv.getChildNodes();

                        for (int k=0; k<netNodes.getLength(); k++) {
                            Node net = netNodes.item(k);
                            String netTag = net.getNodeName();
                            if (netTag.equals("listen-port")) {
                                port = getText(net);
                            }
                            else if (netTag.equals("listen-address")) {
                                addr = getText(net);
                            }
                        }
                        server.setURL(protocol, addr, port);
                    }
                }
                server.setURL(protocol, addr, port);
                servers.add(server);
            }
            else if (tag.equals("configuration-version")) {
                this.version = versionSubstring(getText(node));
            }
            else if (tag.equals("name")) {
                this.domain = getText(node);
            }
            //end 9.0 stuff
            else if (tag.equals("StartupClass")) {
                if (this.version == null) {
                    //6.1 does not have version info
                    //so we guess based on this old tag
                    //that does not exist in 7.0+
                    this.version = WeblogicProductPlugin.VERSION_61;
                    for (int n=0; n<this.servers.size(); n++) {
                        Server server = (Server)this.servers.get(n);
                        if (server.version == null) {
                            server.version = this.version;
                        }
                    }
                }
            }
        }
    }

    private String getText(Node node) {
        return node.getFirstChild().getNodeValue().trim();
    }
    
    private static void testConfig(String file) throws Exception {
        WeblogicConfig cfg = new WeblogicConfig();

        cfg.read(new File(file));

        System.out.println(cfg.version);
        System.out.println(cfg.domain);

        System.out.println("admin=" + cfg.guessAdminServer().name);

        for (int i=0; i<cfg.servers.size(); i++) {
            Server server = (Server)cfg.servers.get(i);
            Properties props = server.getProperties();
            props.put("version", server.getVersion());
            props.list(System.out);
        }
    }

    public static void main(String args[]) throws Exception {
        for (int i=0; i<args.length; i++) {
            System.out.println(args[0] + "...");
            testConfig(args[0]);
        }
    }
}
