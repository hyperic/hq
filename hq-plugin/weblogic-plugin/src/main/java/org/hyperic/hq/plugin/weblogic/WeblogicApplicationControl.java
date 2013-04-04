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

import org.hyperic.hq.bizapp.shared.lather.ControlSendCommandResult_args;
import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.PluginException;


public class WeblogicApplicationControl
    extends WeblogicJMXControl {

    private WeblogicDeployer deployer;

    protected static final String[] ACTIONS = {
        "deploy", "start", "undeploy", "stop"
    };

    private static final List COMMANDS = Arrays.asList(ACTIONS);
    
    public List getActions() {
        return COMMANDS;
    }

    private static final String APP_METRIC =
        WeblogicMetric.template(WeblogicMetric.APPLICATION,
                                WeblogicMetric.APPLICATION_STATE);

    protected String getComponentMetric() {
        return APP_METRIC;
    }

    protected String getPath() {
        try {
            return (String)WeblogicUtil.
                getRemoteMBeanValue(getConfiguredComponentMetric(),
                                    "Path");
        } catch (Exception e) {
            return null;
        }
    }

    protected String getComponentTarget(Metric metric)
        throws IllegalArgumentException {

        String[] targets =
            this.deployer.getComponentTargets(metric);

        String server = getServer();

        for (int i=0; i<targets.length; i++) {
            if (targets[i].equals(server)) {
                //deployed on 1 or more nodes
                return server;
            }
        }

        //deployed on 1 cluster
        if (targets.length == 1) {
            return targets[0];
        }

        //XXX multiple clusters?
        String msg = "Component has multiple targets: " +
            Arrays.asList(targets);

        throw new IllegalArgumentException(msg);
    }

    protected String getServer() {
        return getConfig().getValue(WeblogicMetric.PROP_SERVER);
    }

    protected String getTarget() {
        //XXX
        //return getServer();
        return null;
    }

    protected String getApplication() {
        return getConfig().getValue(WeblogicMetric.PROP_APP);
    }

    protected String getModule() {
        return null; //override to support webapp, ejb, etc.
    }

    public void doAction(String action) throws PluginException {
        if (!COMMANDS.contains(action)) {
            //e.g. webapp.deleteInvalidSessions
            super.doAction(action);
            return;
        }

        this.deployer = new WeblogicDeployer();

        try {
            deployer.configure(getAdminURL(),
                               getAdminUsername(),
                               getAdminPassword());

            //only if we support changing it.
            //deployer.setSource(getPath());

            deployer.setTarget(getTarget());
            deployer.setApplication(getApplication());
            deployer.setModule(getModule());

            deployer.doAction(action);
            setResult(RESULT_SUCCESS);
        } catch (PluginException e) {
            setResult(RESULT_FAILURE);
            setMessage(e.getMessage());
        } catch (IllegalArgumentException e) {
            setResult(RESULT_FAILURE);
            setMessage(e.getMessage());
        }

        this.deployer = null;
    }
}

