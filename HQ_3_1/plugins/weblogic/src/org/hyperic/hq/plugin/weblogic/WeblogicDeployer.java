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

import javax.management.InstanceNotFoundException;

import weblogic.management.ManagementException;
import weblogic.management.WebLogicObjectName;
import weblogic.management.deploy.DeployerRuntime;
import weblogic.management.deploy.DeploymentData;
import weblogic.management.runtime.DeployerRuntimeMBean;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.PluginException;


/**
 * Wrapper class for DeployerRuntimeMBean
 * The DeployerRuntimeMBean is supported in weblogic 7.0+
 */
public class WeblogicDeployer {

    private DeployerRuntimeMBean deployer;
    private String target = null;
    private String source = null;
    private String application = null;
    private String module = null;

    /*
     * fixup ugly weblogic exception messages if possible.
     */
    private final String[] EX_MESSAGES = {
        "Failed to contact",
        "Invalid user name or password",
    };

    private String fixupMessage(Exception e) {
        String msg = e.getMessage();
        for (int i=0; i<EX_MESSAGES.length; i++) {
            if (msg.startsWith(EX_MESSAGES[i])) {
                return EX_MESSAGES[i];
            }
        }
        return msg;
    }

    public void configure(String url,
                          String username,
                          String password)
        throws IllegalArgumentException {
        try {
            this.deployer =
                DeployerRuntime.getDeployerRuntime(username,
                                                   password,
                                                   url);
        } catch (InstanceNotFoundException e) {
            String msg = "Deployer not found at " + url;
            throw new IllegalArgumentException(msg);
        } catch (IllegalArgumentException e) {
            String msg = "Error getting deployer: " + 
                fixupMessage(e);
            throw new IllegalArgumentException(msg);
        }
    }

    public String getTarget() {
        return this.target;
    }

    public void setTarget(String value) {
        this.target = value;
    }

    public void setSource(String value) {
        this.source = value;
    }

    public String getSource() {
        return this.source;
    }

    public void setApplication(String value) {
        this.application = value;
    }

    public String getApplication() {
        return this.application;
    }

    public void setModule(String value) {
        this.module = value;
    }

    public String getModule() {
        return this.module;
    }

    protected String[] getComponentTargets(Metric metric)
        throws IllegalArgumentException {

        WebLogicObjectName[] targets = new WebLogicObjectName[0];

        String err = null;

        try {
            targets = (WebLogicObjectName[])WeblogicUtil.
                getRemoteMBeanValue(metric, "Targets");
        } catch (MetricNotFoundException e) {
            err = "MBean instance not found: " + metric.getObjectName();
        } catch (MetricUnreachableException e) {
            err = e.getMessage();
        } catch (PluginException e) {
            err = e.getMessage();
        }
        
        if (err != null) {
            throw new IllegalArgumentException(err);
        }

        String[] names = new String[targets.length];

        for (int i=0; i<targets.length; i++) {
            names[i] = targets[i].getName();
        }

        return names;
    }

    public void doAction(String action)
        throws PluginException, IllegalArgumentException {

        String target = getTarget();
        String source = getSource();
        String module = getModule();

        String[] modules = (module == null) ?
            null : new String[] { module };

        DeploymentData info = null;
        String app = getApplication();

        //unused
        String staging = null;
        String id = null;

        if (app == null) {
            throw new IllegalArgumentException("application not specified");
        }

        if ((modules != null) && (target == null)) {
            throw new IllegalArgumentException("target not specified");
        }

        if (target != null) {
            info = new DeploymentData();

            info.addTarget(target, modules);
        }

        try {
            if (action.equals("deploy") ||
                action.equals("start")) {
                this.deployer.activate(source, app, staging,
                                       info, id);
            }
            else if (action.equals("undeploy")) {
                this.deployer.unprepare(app, info, id);
            }
            else if (action.equals("stop")) {
                this.deployer.deactivate(app, info, id);
            }
            /* not supported
            else if (action.equals("remove")) {
                this.deployer.remove(app, info, id);
            }
            */
            else {
                String msg = "unsupported action: " + action;
                throw new IllegalArgumentException(msg);
            }
        } catch (ManagementException e) {
            throw new PluginException(e.getMessage(), e);
        }
    }
}
