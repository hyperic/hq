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

package org.hyperic.hq.plugin.jboss;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.util.StringUtil;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Config parsing intended only for JBoss server auto discovery.
 */
public class JBossConfig {
    private static final Log log =
        LogFactory.getLog(JBossConfig.class.getName());

    private static final String NAMING_MBEAN =
        "jboss:service=Naming";

    private static final String BINDING_MBEAN =
        "jboss.system:service=ServiceBindingManager";

    private static final String WEBSERVER_MBEAN =
        "jboss.web:service=WebServer";

    private static HashMap cache = null;
    private File serviceXML;
    private File bindingXML;
    private String jnpPort;
    private String jnpPortBinding;
    private String jnpAddress;
    private String jnpAddressBinding;
    private String httpPort;
    private String serverBinding;
    private long lastModified;
    private long lastModifiedBinding;

    private JBossConfig(File serviceXML) {
        this.serviceXML = serviceXML;
        this.lastModified = this.serviceXML.lastModified();
    }

    public static synchronized JBossConfig getConfig(File configXML) {
        if (cache == null) {
            cache = new HashMap();
        }

        JBossConfig cfg =
            (JBossConfig)cache.get(configXML);

        if ((cfg == null) || cfg.hasChanged()) {
            if (cfg == null) {
                log.debug("Parsing unseen: " + configXML);
            }
            else {
                log.debug("Parsing modified: " + configXML);
            }

            cfg = new JBossConfig(configXML);
            cache.put(configXML, cfg);

            Document doc = null;
            try {
                doc = cfg.parse(cfg.serviceXML);
                cfg.findJnpPort(doc);
            } catch (IOException e) {
                log.error("Error parsing " + cfg.serviceXML, e);
            } finally {
                doc = null;
            }
        }
        else {
            log.debug("Unchanged: " + configXML);
        }

        return cfg;
    }

    private boolean hasChanged() {
        long lastModified = this.serviceXML.lastModified();
        if (lastModified != this.lastModified) {
            this.lastModified = lastModified;
            return true;
        }

        if (this.bindingXML != null) {
            lastModified = this.bindingXML.lastModified();
            if (lastModified != this.lastModifiedBinding) {
                return true;
            }
        }

        return false;
    }

    public String getHttpPort() {
        return this.httpPort;
    }

    public String getJnpPort() {
        String port = 
            (this.jnpPortBinding == null) ?
             this.jnpPort : this.jnpPortBinding;
        if (port == null) {
            return "1099";
        }
        else {
            return port;
        }
    }

    public String getJnpAddress() {
        String addr =
            (this.jnpAddressBinding == null) ?
             this.jnpAddress : this.jnpAddressBinding;

        if ((addr == null) ||
             addr.equals("${jboss.bind.address}"))
        {
            return "127.0.0.1";
        }
        else {
            return addr;
        }
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

    private String getText(Node node) {
        return node.getFirstChild().getNodeValue().trim();
    }

    private static boolean hasValue(String val) {
        return (val != null) && (val.length() != 0);
    }

    private File getJBossHome(File configXML) {
        File home = configXML.getParentFile();
        while (home != null) {
            boolean isServer = home.getName().equals("server");
            home = home.getParentFile();
            if (isServer) {
                break;
            }
        }
        return home;
    }

    private boolean findJnpPortBinding(Document doc) {
        boolean foundPort = false;
        NodeList servers =
            doc.getDocumentElement().getElementsByTagName("server");

        for (int i=0; i<servers.getLength(); i++) {
            Node server = servers.item(i);

            if (!server.hasAttributes()) {
                continue;
            }

            String name = getAttribute(server, "name");

            if (!name.equals(this.serverBinding)) {
                continue;
            }

            NodeList configs = server.getChildNodes();
            for (int j=0; j<configs.getLength(); j++) {
                Node config = configs.item(j);

                if (!config.hasAttributes()) {
                    continue;
                }

                if (!"service-config".equals(config.getNodeName())) {
                    continue;
                }

                name = getAttribute(config, "name");
                boolean isNaming=false, isWebserver=false;
                if (!((isNaming = name.equals(NAMING_MBEAN)) ||
                      (isWebserver = name.equals(WEBSERVER_MBEAN))))
                {
                    continue;
                }

                NodeList bindings = config.getChildNodes();
                for (int k=0; k<bindings.getLength(); k++) {
                    Node binding = bindings.item(k);

                    if (!"binding".equals(binding.getNodeName())) {
                        continue;
                    }

                    if (binding.hasAttributes()) {
                        String val = getAttribute(binding, "port");
                        log.debug(this.serverBinding + " " +
                                  name + "=" + val);
                        if (hasValue(val)) {
                            if (isNaming) {
                                this.jnpPortBinding = val;
                                foundPort = true;
                            }
                            else if (isWebserver) {
                                this.httpPort = val;
                            }
                        }
                        val = getAttribute(binding, "host");
                        if (hasValue(val)) {
                            if (isNaming) {
                                this.jnpAddressBinding = val;
                            }
                        }
                    }
                    break;
                }
            }
        }

        return foundPort;
    }

    private boolean findJnpPortBinding(Node mbean)
        throws IOException {

        NodeList attrs = mbean.getChildNodes();

        for (int i=0; i<attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            if (!attr.hasAttributes()) {
                continue;
            }

            String attrName = getAttribute(attr, "name");

            if ("ServerName".equals(attrName)) {
                this.serverBinding = getText(attr);
                log.debug("ServerName=" + this.serverBinding);
            }
            else if ("StoreURL".equals(attrName)) {
                String url = getText(attr);

                File home = getJBossHome(this.serviceXML);
                if (home != null) {
                    log.debug("jboss.home.url=" + home);
                    url = StringUtil.replace(url,
                                             "${jboss.home.url}",
                                             home.toString());
                }

                url = StringUtil.replace(url,
                                         "${jboss.server.config.url}",
                                         this.serviceXML.getParentFile().toString());

                File bindings = new File(url);
                boolean exists = bindings.exists();
                log.debug("StoreURL exists=" + exists + " (" + url + ")");

                if (exists) {
                    this.bindingXML = bindings;
                    this.lastModifiedBinding =
                        bindings.lastModified();

                    Document bindingDoc;
                    try {
                        bindingDoc = parse(bindings);
                        return findJnpPortBinding(bindingDoc);
                    } finally {
                        bindingDoc = null;
                    }
                }
            }
        }

        return false;
    }

    private boolean findJnpPort(Node mbean) {
        NodeList attrs = mbean.getChildNodes();
        boolean foundPort = false;

        for (int i=0; i<attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            if (!attr.hasAttributes()) {
                continue;
            }
            String attrName = getAttribute(attr, "name");
            if ("Port".equals(attrName)) {
                String val = getText(attr);
                if (hasValue(val)) {
                    this.jnpPort = val;
                    foundPort = true;
                }
            }
            else if ("BindAddress".equals(attrName)) {
                String val = getText(attr);
                if (hasValue(val)) {
                    this.jnpAddress = val;
                }
            }
        }

        return foundPort;
    }

    private void findJnpPort(Document doc)
        throws IOException {

        NodeList mbeans =
            doc.getDocumentElement().getElementsByTagName("mbean");

        for (int i=0; i<mbeans.getLength(); i++) {
            Node mbean = mbeans.item(i);

            if (!mbean.hasAttributes()) {
                continue;
            }

            String name = getAttribute(mbean, "name");
            if (NAMING_MBEAN.equals(name)) {
                findJnpPort(mbean);
                log.debug(NAMING_MBEAN + " port=" + this.jnpPort +
                          ", host=" + this.jnpAddress);
            }
            else if (BINDING_MBEAN.equals(name)) {
                findJnpPortBinding(mbean);
                log.debug(BINDING_MBEAN + " port=" + this.jnpPortBinding +
                          ", host=" + this.jnpAddressBinding);
            }
        }

        log.debug("JNP port=" + getJnpPort());
    }

    private Document parse(File file)
        throws IOException {

        FileInputStream fis = null;

        try {
            fis = new FileInputStream(file);
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder parser = factory.newDocumentBuilder();
            return parser.parse(new InputSource(fis));
        } catch (ParserConfigurationException e) {
            throw new IllegalArgumentException(e.getMessage());
        } catch (SAXException e) {
            throw new IllegalArgumentException(e.getMessage());
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {}
            }
        }
    }

    public static void main(String args[]) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: <config file>");
            System.exit(1);
        }

        JBossConfig cfg = JBossConfig.getConfig(new File(args[0]));

        System.out.println("JNP url: jnp://" +
                           cfg.getJnpAddress() + ":" +
                           cfg.getJnpPort());
        if (cfg.getHttpPort() != null) {
            System.out.println("HTTP port: " + cfg.getHttpPort());
        }
    }
}
