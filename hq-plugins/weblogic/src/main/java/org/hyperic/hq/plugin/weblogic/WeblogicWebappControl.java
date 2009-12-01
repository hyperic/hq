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

import java.util.List;
import java.util.Arrays;
import java.util.ArrayList;

import org.hyperic.hq.product.Metric;

public class WeblogicWebappControl
    extends WeblogicApplicationControl {

    private static final List COMMANDS;

    static {
        ArrayList actions = new ArrayList();
        actions.addAll(Arrays.asList(ACTIONS));
        actions.add("deleteInvalidSessions");

        COMMANDS = actions;
    }

    private Metric mbean;

    private static final String WEBAPP_MBEAN =
        WeblogicMetric.template(WeblogicMetric.WEBAPP_COMPONENT,
                             "Targets");

    public List getActions() {
        return COMMANDS;
    }

    private Metric getMBean() {
        if (this.mbean == null) {
            this.mbean = configureMetric(WEBAPP_MBEAN);
        }

        return this.mbean;
    }

    protected String getComponentMetric() {
        String objectName =
            WeblogicMetric.getObjectTemplate(this,
                                             "WebAppComponentRuntime");
        String attr =
            WeblogicMetric.WEBAPP_COMPONENT_RUNTIME_STATUS;

        return WeblogicMetric.template(objectName, attr);
    }

    protected boolean convertIsRunning(Object value) {
        return ((String)value).equals("DEPLOYED");
    }

    protected String getTarget() {
        return getComponentTarget(getMBean());
    }

    protected String getModule() {
        return getConfig().getValue(WeblogicMetric.PROP_WEBAPP);
    }
}
