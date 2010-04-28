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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.SAXParser;

import org.xml.sax.SAXException;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Resin config parser (resin.conf)
 */
public class ResinConfig {

    private static HashMap cache = null;
    private ArrayList servers = new ArrayList();
    private long lastModified = 0;

    private ResinConfig() {}

    public static synchronized ResinConfig getConfig(File resinConf) {
        if (cache == null) {
            cache = new HashMap();
        }

        ResinConfig cfg = (ResinConfig)cache.get(resinConf);

        long lastModified = resinConf.lastModified();

        if ((cfg == null) || (lastModified != cfg.lastModified)) {
            cfg = new ResinConfig();
            cfg.lastModified = lastModified;
            cache.put(resinConf, cfg);

            try {
                cfg.read(resinConf);
            } catch (IOException e) {
            }
        }

        return cfg;
    }

    public List getServers() {
        return servers;
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
        ResinConfigHandler handler = new ResinConfigHandler();
        parser.parse(is, handler);

        servers = handler.getServers();
    }

    private class ResinConfigHandler extends DefaultHandler {

        private ArrayList servers = new ArrayList();

        public void startElement(String uri,
                                 String localName,
                                 String qName,
                                 Attributes attributes)
            throws SAXException {

            if (!qName.equals("http")) {
                return;
            }

            ResinServer server = new ResinServer();

            server.host = attributes.getValue("host");
            if (server.host == null || server.host.equals("*")) {
                server.host = "localhost";
            }

            server.port = attributes.getValue("port");
            server.id = attributes.getValue("id");
            if (server.id == null) {
                //Resin 3.x changed this attribute
                server.id = attributes.getValue("server-id");
            }

            // XXX: Store other attribues for cprops

            servers.add(server);
        }

        public ArrayList getServers() {
            return servers;
        }
    }
}
