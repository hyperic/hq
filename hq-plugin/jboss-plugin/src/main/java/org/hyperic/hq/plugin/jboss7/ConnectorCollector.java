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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.jboss7.objects.Connector;
import org.hyperic.hq.product.PluginException;

public class ConnectorCollector extends JBoss7DefaultCollector {

    private static final Log log = LogFactory.getLog(ConnectorCollector.class);

    @Override
    public void collect(JBossAdminHttp admin) {
        String connector = (String) getProperties().get("connector");
        try {
            Connector c = admin.getConnector(connector);
            setAvailability(true);
            setValue("bytesReceived", c.getBytesReceived());
            setValue("bytesSent", c.getBytesSent());
            setValue("errorCount", c.getErrorCount());
            setValue("maxTime", c.getMaxTime());
            setValue("processingTime", c.getProcessingTime());
            setValue("requestCount", c.getRequestCount());
        } catch (PluginException ex) {
            setAvailability(false);
            log.debug(ex.getMessage(), ex);
        }
    }

    @Override
    public Log getLog() {
        return log;
    }
}
