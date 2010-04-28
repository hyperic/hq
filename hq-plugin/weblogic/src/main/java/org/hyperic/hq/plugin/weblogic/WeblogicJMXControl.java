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

import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;

import org.hyperic.util.config.ConfigResponse;

/**
 * Base class for Weblogic control via JMX.
 */
public abstract class WeblogicJMXControl
    extends ControlPlugin
    implements WeblogicAction {

    protected static final String STRING_CLASS =
        String.class.getName();

    private Metric componentMetric = null;

    private WeblogicAuthControl authControl;

    public WeblogicJMXControl() {
        super();
        setName(WeblogicProductPlugin.NAME);
    }

    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);

        this.authControl = new WeblogicAuthControl(this,
                                                   getConfiguredComponentMetric());
    }

    protected String getAdminURL() {
        return getConfig().getValue(WeblogicMetric.PROP_ADMIN_URL);
    }

    protected String getAdminUsername() {
        return getConfig().getValue(WeblogicMetric.PROP_ADMIN_USERNAME);
    }

    protected String getAdminPassword() {
        return getConfig().getValue(WeblogicMetric.PROP_ADMIN_PASSWORD);
    }

    protected Metric configureMetric(String template) {
        template = WeblogicMetric.translateNode(template, getConfig());

        String metric = Metric.translate(template, getConfig());

        getLog().debug("configureMetric=" + metric);

        try {
            return Metric.parse(metric); //parsing will be cached
        } catch (Exception e) {
            e.printStackTrace(); //XXX; aint gonna happen
            return null;
        }
    }

    protected abstract String getComponentMetric();

    protected Metric getConfiguredComponentMetric() {
        if (this.componentMetric == null) {
            this.componentMetric = configureMetric(getComponentMetric());
        }

        return this.componentMetric;
    }

    protected boolean convertIsRunning(Object value) {
        return ((Boolean)value).booleanValue();
    }

    protected boolean isRunning() {
        return this.authControl.isRunning();
    }

    public boolean isWeblogicRunning() {
        Metric metric = getConfiguredComponentMetric();

        Object value;

        try {
            value = WeblogicUtil.getRemoteMBeanValue(metric);
            getLog().debug("isRunning: " + metric + "=" + value);
        } catch (Exception e) {
            getLog().trace("isRunning: " + metric, e);
            return false;
        }

        return convertIsRunning(value);
    }

    protected String getInvokeMethod(String action) {
        return action;
    }

    public void doAction(String action) {
        this.authControl.doAction(action);
    }

    public void doWeblogicAction(String action) {
        Metric mbean = getConfiguredComponentMetric();
        String method = getInvokeMethod(action);

        Object[] args = new Object[0];
        String[] sig  = new String[0];

        try {
            Object obj =
                WeblogicUtil.invoke(mbean, method, args, sig);
            setResult(RESULT_SUCCESS);
            if (obj != null) {
                setMessage(obj.toString());
            }
        } catch (Exception e) { //XXX
            setResult(RESULT_FAILURE);
            setMessage(e.getMessage());
            getLog().debug("doAction: invoke failed", e);
        }

        getLog().debug("doAction: result=" + getResult());
    }
}
