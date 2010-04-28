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

package org.hyperic.hq.plugin.dotnet;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.Win32MeasurementPlugin;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;

public class DotNetMeasurementPlugin
    extends Win32MeasurementPlugin {

    private static final String DATA_DOMAIN = ".NET CLR Data";
    private static final String DATA_PREFIX = "SqlClient: ";
    private static final String RUNTIME_NAME = "_Global_";

    protected String getAttributeName(Metric metric) {
        //avoiding Metric parse errors on ':' in DATA_PREFIX.
        if (metric.getDomainName().equals(DATA_DOMAIN)) {
            return DATA_PREFIX + metric.getAttributeName();
        }
        else {
            return metric.getAttributeName();
        }
    }

    public String translate(String template, ConfigResponse config) {
        final String prop = DotNetDetector.PROP_APP;

        //undo escape for plugin linter
        template = StringUtil.replace(template,
                                      "__percent__", "%");

        return StringUtil.replace(template,
                                  "${" + prop + "}", 
                                  config.getValue(prop,
                                                  RUNTIME_NAME));
    }
}
