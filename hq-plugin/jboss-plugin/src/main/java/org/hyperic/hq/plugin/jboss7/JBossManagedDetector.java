/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2011], Hyperic, Inc.
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
package org.hyperic.hq.plugin.jboss7;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.logging.Log;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ServerResource;
import org.hyperic.util.config.ConfigResponse;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class JBossManagedDetector extends JBossHostControllerDetector {

    private final Log log = getLog();

    @Override
    public List getServerResources(ConfigResponse platformConfig) throws PluginException {
        List<ServerResource> servers = new ArrayList<ServerResource>();
        List<ServerResource> controllers = super.getServerResources(platformConfig);
        for (ServerResource controller : controllers) {
            List<String> serversNames = getServersNames(controller.getIdentifier());
            for (String serverName : serversNames) {
                ServerResource server = createServerResource(controller.getInstallPath());
                server.setIdentifier(serverName + "-" + controller.getIdentifier());
                ConfigResponse pc = controller.getProductConfig();
                pc.setValue(SERVER, serverName);
                setProductConfig(server, pc);
                setControlConfig(server, new ConfigResponse());
                server.setName(prepareServerName(server.getProductConfig()));
                servers.add(server);
            }
        }
        return servers;
    }

    private List<String> getServersNames(String config) {
        List<String> names = new ArrayList<String>();
        File cfgFile = new File(config);

        try {
            log.debug("[getServerProductConfig] cfgFile=" + cfgFile.getCanonicalPath());
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = (Document) dBuilder.parse(cfgFile);

            XPathFactory factory = XPathFactory.newInstance();
            XPathExpression expr = factory.newXPath().compile("//host/servers/server");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodeList = (NodeList) result;

            for (int i = 0; i < nodeList.getLength(); i++) {
                names.add(nodeList.item(i).getAttributes().getNamedItem("name").getNodeValue());
            }
        } catch (Exception ex) {
            log.debug("Error discovering the servers names : " + ex, ex);
        }
        return names;
    }

    @Override
    boolean haveServices() {
        return true;
    }
}
