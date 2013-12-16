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

import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.hyperic.hq.product.ControlPlugin;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;

/**
 * Application control for WebSphere 5.0
 */
public class WebsphereAppControl extends ControlPlugin {

    private static final String[] ACTIONS = {
        "start", "stop"
    };
    private static final List COMMANDS = Arrays.asList(ACTIONS);
    private static final String APP_MANAGER_TMPL =
            "WebSphere:"
            + "name=ApplicationManager,"
            + "mbeanIdentifier=ApplicationManager,"
            + "type=ApplicationManager,"
            + "node=%server.node%,"
            + "process=%server.name%,*";
    private String appManagerName = null;
    private Properties jmxProps;
    private static final String STRING_CLASS =
            String.class.getName();
    private static final String[] APP_SIG = {
        STRING_CLASS,};

    @Override
    public List getActions() {
        return COMMANDS;
    }

    private String getAppManagerName() {
        if (this.appManagerName == null) {
            this.jmxProps = getConfig().toProperties();

            this.appManagerName =
                    Metric.translate(APP_MANAGER_TMPL, getConfig());

            getLog().debug("appManagerName=" + this.appManagerName);
        }

        return this.appManagerName;
    }

    @Override
    protected boolean isRunning() {
        return false; //XXX
    }

    @Override
    public void doAction(String action)
            throws PluginException {
        String appManager = getAppManagerName();

        String name =
                getConfig().getValue(WebsphereProductPlugin.PROP_APP_NAME);
        Object[] args = new Object[]{name};

        String method;

        if (action.equals("start")) {
            method = "startApplication";
        } else if (action.equals("stop")) {
            method = "stopApplication";
        } else {
            throw new PluginException("unsupported action=" + action);
        }

        getLog().debug("doAction: action=" + action
                + ", method=" + method
                + ", appManager=" + appManager);

        try {
            this.jmxProps.list(System.out);

            WebsphereUtil.invoke(appManager,
                    this.jmxProps,
                    method,
                    args, APP_SIG);
            setResult(RESULT_SUCCESS);
        } catch (PluginException e) {
            setResult(RESULT_FAILURE);
            getLog().debug("doAction: invoke failed", e);
            throw new PluginException(e.getMessage(), e);
        }

        getLog().debug("doAction: result=" + getResult());
    }
}
