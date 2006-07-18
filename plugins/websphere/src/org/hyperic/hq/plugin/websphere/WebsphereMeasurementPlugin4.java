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

package org.hyperic.hq.plugin.websphere;

import java.io.File;

import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.SchemaBuilder;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.hq.plugin.websphere.ejs.WebsphereRemote;

public class WebsphereMeasurementPlugin4 
    extends WebsphereMeasurementPlugin {

    protected double getAvailValue(Metric metric) {
        return WebsphereRemote.isRunning(metric) ?
            Metric.AVAIL_UP :
            Metric.AVAIL_DOWN;
    }

    public ConfigSchema getConfigSchema(TypeInfo info,
                                        ConfigResponse config) {

        SchemaBuilder schema = new SchemaBuilder(config);

        switch (info.getType()) {
          case TypeInfo.TYPE_SERVER:
            if (info.getName().indexOf("Admin") == -1) {
                schema.add(WebsphereProductPlugin.PROP_SERVER_PORT,
                           "Application Server Port",
                           "9080");
            }
            break;
          case TypeInfo.TYPE_SERVICE:
            if (info.isService(WebsphereProductPlugin.WEBAPP_NAME)) {
                schema.add(WebsphereProductPlugin.PROP_WEBAPP_DISPLAY_NAME,
                           "Webapp Display Name",
                           "Examples Application");
                schema.add(WebsphereProductPlugin.PROP_WEBAPP_CONTEXT,
                           "Webapp Context Name",
                           File.separator+"examples");
            }
            else if (info.isService(WebsphereProductPlugin.EJB_NAME)) {
                schema.add(WebsphereProductPlugin.PROP_EJB_JNDI_NAME,
                           "EJB JNDI Name",
                           "IncBean");
            }
            break;
        }

        return schema.getSchema();
    }
}
