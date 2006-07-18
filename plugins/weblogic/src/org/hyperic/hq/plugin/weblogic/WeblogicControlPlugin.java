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

import java.io.File;

import java.util.List;
import java.util.Arrays;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.ConfigSchema;

public class WeblogicControlPlugin
    extends ServerControlPlugin
    implements WeblogicAction {

    //10 minutes ought to be plenty
    public static final int DEFAULT_TIMEOUT = 60 * 10;

    private static final String[] actions = {
        "start", "stop", "runGarbageCollector"
    };

    private static final List commands = Arrays.asList(actions);

    protected static final String DEFAULT_SCRIPT = "startWebLogic.sh";

    static final String SERVER_METRIC = 
        WeblogicMetric.template(WeblogicMetric.SERVER_RUNTIME,
                                WeblogicMetric.SERVER_RUNTIME_STATE);

    //the Server "configuration" MBean use for start via node manager
    static final String ADMIN_METRIC = 
        WeblogicMetric.template("%domain%:Name=%server%,Type=Server",
                             WeblogicMetric.SERVER_RUNTIME_STATE);

    private WeblogicAuthControl authControl;

    private Metric lifecycleMetric = null;
    private Metric adminMetric = null;
    private Metric jvmMetric = null;

    public WeblogicControlPlugin() {
        super();
        //give waitForState enough time
        setTimeout(DEFAULT_TIMEOUT);
        setControlProgram(DEFAULT_SCRIPT);
    }

    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);

        /*
          XXX: enable again
        File script = new File(getControlProgram());
        if (!script.exists()) {
            throw new PluginException("Weblogic startup script " +
                                             "not found");
        }
        */

        this.authControl = new WeblogicAuthControl(this,
                                                   getLifecycleMetric());
    }

    public ConfigSchema getConfigSchema(TypeInfo info,
                                        ConfigResponse config) {
        ConfigSchema schema = super.getConfigSchema(info, config);

        //control program is optional for Node servers
        //when empty the NodeManager is used to start them.
        if (info.isServer(WeblogicProductPlugin.SERVER_NAME)) {
            ConfigOption option =
                schema.getOption(WeblogicControlPlugin.PROP_PROGRAM);
            if (option != null) {
                option.setOptional(true);
            }
        }

        return schema;
    }

    public List getActions() {
        return commands;
    }

    private Metric configureMetric(String template) {
        template = WeblogicMetric.translateNode(template, getConfig());

        String metric = Metric.translate(template, getConfig());

        getLog().debug("configureMetric=" + metric);

        try {
            return Metric.parse(metric); //parsing will be cached
        } catch (Exception e) {
            e.printStackTrace(); //XXX; but aint gonna happen
            return null;
        }
    }

    protected Metric getLifecycleMetric() {
        if (this.lifecycleMetric == null) {
            this.lifecycleMetric = configureMetric(SERVER_METRIC);
        }
        
        return this.lifecycleMetric;
    }

    private Metric getJVMMetric() {
        if (this.jvmMetric == null) {
            String jvmObjectName =
                WeblogicMetric.getObjectTemplate(this, "JVMRuntime");
            String jvmMetric = 
                WeblogicMetric.template(jvmObjectName, "Name");

            this.jvmMetric = configureMetric(jvmMetric);
        }
        
        return this.jvmMetric;
    }

    protected Metric getAdminMetric() {
        if (this.adminMetric == null) {

            String metric =
                Metric.translate(ADMIN_METRIC, getConfig());

            getLog().debug("adminMetric=" + metric);

            try {
                return Metric.parse(metric); //parsing will be cached
            } catch (Exception e) {
                e.printStackTrace(); //XXX; but aint gonna happen
                return null;
            }
        }
        
        return this.adminMetric;
    }

    protected boolean isRunning() {
        return this.authControl.isRunning();
    }

    public boolean isWeblogicRunning() {
        Metric metric = getLifecycleMetric();

        Integer state;

        try {
            state = (Integer)WeblogicUtil.getRemoteMBeanValue(metric);
        } catch (Exception e) {
            return false;
        }

        double val = WeblogicUtil.convertStateVal(state);

        return val == Metric.AVAIL_UP;
    }

    protected boolean isBackgroundCommand() {
        return true;
    }

    protected void start() {
        File script = new File(getControlProgram());
        if (!script.exists()) {
            setResult(RESULT_FAILURE);
            setMessage("Control program does not exist: " +
                        getControlProgram());
            return;
        }

        if (isWeblogicRunning()) {
            setResult(RESULT_FAILURE);
            setMessage("Server already running");
            return;
        }

        super.start(null); //requires a script

        if (isWeblogicRunning()) {
            setResult(RESULT_SUCCESS);
            setMessage("Server started");
            return;
        }

        setResult(RESULT_FAILURE);

        if (getMessage() == null) {
            setMessage("Server did not start");
        }
    }

    protected Object invokeAction(Metric metric, String action) {
        try {
            Object obj = WeblogicUtil.invoke(metric, action);
            setResult(RESULT_SUCCESS);
            if (obj != null) {
                setMessage(obj.toString());
            }
            return obj;
        } catch (MetricNotFoundException e) {
            setMessage(e.getMessage());
            setResult(RESULT_FAILURE);
        } catch (MetricUnreachableException e) {
            setMessage(e.getMessage());
            setResult(RESULT_FAILURE);
        } catch (PluginException e) {
            if (action.equals("shutdown") &&
                !isRunning())
            {
                //might get nested java.net.SocketException: Connection reset
                //which is ok/expected-ish if we are stopping the server
                setResult(RESULT_SUCCESS);
            }
            else {
                setMessage(e.getMessage());
                setResult(-2);
            }
        }
        return null;
    }

    public void doAction(String action) {
        this.authControl.doAction(action);
    }

    public void doWeblogicAction(String action) {
        if (action.equals("start")) {
            start();
            return;
        }

        Metric metric;

        String method;

        if (action.equals("stop")) {
            metric = getLifecycleMetric();
            method = "shutdown";
        }
        else if (action.equals("runGarbageCollector")) {
            metric = getJVMMetric();
            method = "runGC";
        }
        else {
            return; //ControlPluginManager checks for valid actions
        }

        invokeAction(metric, method);
    }
}
