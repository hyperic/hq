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

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.logging.Log;
import org.hyperic.util.config.ConfigResponse;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public class JBossHostControllerDetector extends JBossDetectorBase {

    private final Log log = getLog();

    @Override
    void setUpExtraProductConfig(ConfigResponse cfg, Document doc) throws XPathException {
        XPathFactory factory = XPathFactory.newInstance();
        XPathExpression expr = factory.newXPath().compile(getConfigRoot());
        Object result = expr.evaluate(doc, XPathConstants.NODESET);
        NodeList nodeList = (NodeList) result;
        String name = nodeList.item(0).getAttributes().getNamedItem("name").getNodeValue();
        cfg.setValue(HOST, name);
    }

    @Override
    String getPidsQuery() {
        return "State.Name.sw=java,Args.*.eq=org.jboss.as.host-controller";
    }

    @Override
    String getConfigRoot() {
        return "//host";
    }

    @Override
    String getDefaultConfigName() {
        return "host.xml";
    }

    @Override
    String getDefaultConfigDir() {
        return "/domain";
    }

    @Override
    boolean haveServices() {
        return false;
    }
}
