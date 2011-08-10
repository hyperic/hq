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

import org.hyperic.hq.plugin.jboss.jmx.JBossQuery;
import org.hyperic.hq.product.Metric;

public class JBoss6StateServiceControlPlugin
        extends JBossStateServiceControlPlugin {

    public static final String CONTROL_OBJECT_NAME = "CONTROL_OBJECT_NAME";

    @Override
    protected String getAttribute() {
        return "state";
    }

    @Override
    protected String getObjectName() {
        //defined in hq-plugin.xml within the <service> tag
        String objectName = getTypeProperty(CONTROL_OBJECT_NAME);
        if (objectName == null) {
            //programmer error.
            String msg =
                    JBossQuery.PROP_OBJECT_NAME + " property undefined for "
                    + getTypeInfo().getName();
            throw new IllegalArgumentException(msg);
        }
        return objectName;
    }

    @Override
    protected boolean isRunning() {
        boolean running = false;
        Metric metric = getConfiguredMetric();
        getLog().debug("[isRunning] metric=" + metric);
        Integer value;

        try {
            value = (Integer) JBossUtil.getRemoteMBeanValue(metric);
            getLog().debug("[isRunning] value='" + value+"'");
            running = value.intValue()==1;
        } catch (Exception e) {
            getLog().debug("isRunning: " + e.getMessage(), e);
        }

        getLog().debug("[isRunning] running=" + running);
        return running;
    }
}
