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

import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.hq.plugin.jboss7.objects.Deployment;
import org.hyperic.hq.product.CollectorResult;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricValue;
import org.hyperic.hq.product.PluginException;

public class DeploymentCollector extends JBoss7DefaultCollector {

    private static final Log log = LogFactory.getLog(ConnectorCollector.class);

    @Override
    public void collect(JBossAdminHttp admin) {
        String connector = (String) getProperties().get("connector");
        try {
            List<Deployment> deployments = admin.getDeployments();
            for (Deployment d : deployments) {
                String name = d.getName();
                setValue(name + "." + Metric.ATTR_AVAIL, d.getEnabled() ? Metric.AVAIL_UP : Metric.AVAIL_DOWN);
            }
        } catch (PluginException ex) {
            setAvailability(false);
            log.debug(ex.getMessage(), ex);
        }
    }

    @Override
    public MetricValue getValue(Metric metric, CollectorResult result) {
        MetricValue res = result.getMetricValue(metric.getAttributeName());
        if (metric.getAttributeName().endsWith(Metric.ATTR_AVAIL)) {
            if (res.getValue() != Metric.AVAIL_UP) {
                res = new MetricValue(Metric.AVAIL_DOWN, System.currentTimeMillis());
            }
            log.debug("[getValue] Member=" + metric.getObjectProperty("member.name") + " metric=" + metric.getAttributeName() + " res=" + res.getValue());
        }
        return res;
    }

    @Override
    public Log getLog() {
        return log;
    }
}
