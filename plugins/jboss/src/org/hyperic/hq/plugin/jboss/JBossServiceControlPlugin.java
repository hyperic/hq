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

import java.util.Arrays;

import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricInvalidException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.plugin.jboss.jmx.JBossQuery;

public class JBossServiceControlPlugin extends ControlPlugin {

    static final String DEFAULT_ATTRIBUTE =
        JBossMeasurementPlugin.ATTR_STATE;

    private Metric configuredMetric;

    protected String getAttribute() {
        String attr = getTypeProperty(JBossQuery.PROP_ATTRIBUTE_NAME);
        if (attr != null) {
            return attr;
        }
        return DEFAULT_ATTRIBUTE;
    }
    
    private String getTemplate() {
        return
            getObjectName() + ":" +
            getAttribute() + ":" + 
            getProperty(JBossMeasurementPlugin.PROP_TEMPLATE_CONFIG);
    }
    
    protected Metric getConfiguredMetric() {
        if (configuredMetric == null) {
            String template = getTemplate();
            configuredMetric = JBossUtil.configureMetric(this, template);
            getLog().debug("Configured metric=" + configuredMetric);
        }
        
        return configuredMetric;
    }

    protected String getObjectName() {
        //defined in hq-plugin.xml within the <service> tag
        String objectName = getTypeProperty(JBossQuery.PROP_OBJECT_NAME);
        if (objectName == null) {
            //programmer error.
            String msg =
                JBossQuery.PROP_OBJECT_NAME + " property undefined for " +
                getTypeInfo().getName();
            throw new IllegalArgumentException(msg);
        }
        return objectName;
    }
    
    public void doAction(String action, String[] args)
        throws PluginException {

        invokeMethod(action, args);
    }

    protected void invokeMethod(String action) {
        invokeMethod(action, new String[0]);
    }

    protected void invokeMethod(String action, String[] args) {
        getLog().debug("invoking " + action +
                " " + Arrays.asList(args));

        try {
            Object obj =
                JBossUtil.invoke(getConfiguredMetric(), action,
                                 args, new String[0]);
            getLog().debug(getConfiguredMetric() + "." + action +
                           "() returned: " + obj);
            setResult(RESULT_SUCCESS);
            if (obj != null) {
                setMessage(obj.toString());
            }
        } catch (MetricInvalidException e) {
            getLog().error(e.getMessage(), e);
            setMessage(e.getMessage());
            setResult(RESULT_FAILURE);
        } catch (MetricUnreachableException e) {
            getLog().error(e.getMessage(), e);
            setMessage(e.getMessage());
            setResult(RESULT_FAILURE);
        } catch (PluginException e) {
            getLog().error(e.getMessage(), e);
            setMessage(e.getMessage());
            setResult(-2);
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
            // anything not explicitly thrown by JBossUtil.invoke
            // needs to have the full stack trace logged for debugging.
            setMessage(e.getMessage());
            setResult(-2);
        }
    }
}
