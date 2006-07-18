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

package org.hyperic.hq.plugin.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileInputStream;

import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Config parsing intended only for Tomcat server auto discovery.
 */
public class TomcatConfig {

    private static HashMap cache = null;
    private String port;
    private long lastModified = 0;

    private TomcatConfig() {}

    public static synchronized TomcatConfig getConfig(File configXML) {
        if (cache == null) {
            cache = new HashMap();
        }

        TomcatConfig cfg = (TomcatConfig)cache.get(configXML);

        long lastModified = configXML.lastModified();

        if ((cfg == null) || (lastModified != cfg.lastModified)) {
            cfg = new TomcatConfig();
            cfg.lastModified = lastModified;
            cache.put(configXML, cfg);

            try {
                cfg.read(configXML);
            } catch (IOException e) {
            }
        }

        return cfg;
    }

    public String getPort() {
        return port;
    }

    private void read(File file)
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
    }

    private void parse(InputStream is)
        throws IOException,
               SAXException,
               ParserConfigurationException {

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        TomcatConnectorHandler handler = new TomcatConnectorHandler();
        parser.parse(is, handler);

        port = handler.getPort();
    }

    private class TomcatConnectorHandler extends DefaultHandler {

        private String port;

        public void startElement(String uri,
                                 String localName,
                                 String qName,
                                 Attributes attributes)
            throws SAXException {

            if (!qName.equals("Connector")) {
                return;
            }

            if (attributes.getValue("protocolHandlerClassName") != null) {
                //e.g. org.apache.jk.server.JkCoyoteHandler
                return;
            }

            if (attributes.getValue("protocol") != null) {
                //e.g. AJP/1.3
                return;
            }

            String scheme = attributes.getValue("scheme");
            if ("https".equals(scheme)) {
                return;
            }

            String className = attributes.getValue("className");
            if (className != null) {
                if (className.endsWith("WarpConnector") || //e.g. 4.0.x
                    className.endsWith("Ajp13Connector"))
                {
                    return;
                }
            }

            this.port = attributes.getValue("port");
        }

        protected String getPort() {
            return port;
        }
    }

    public static void main(String args[]) throws Exception {
        for (int i=0; i<args.length; i++) {
            TomcatConfig cfg = TomcatConfig.getConfig(new File(args[i]));

            System.out.println("Port=" + cfg.getPort() + " [" + args[i] + "]");
        }
    }
}
