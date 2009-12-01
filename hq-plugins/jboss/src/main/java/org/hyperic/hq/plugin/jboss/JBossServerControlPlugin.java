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

import java.io.File;
import java.rmi.RemoteException;
import java.util.List;

import javax.naming.NamingException;

import org.hyperic.hq.product.Metric;
import org.hyperic.hq.product.MetricNotFoundException;
import org.hyperic.hq.product.MetricUnreachableException;
import org.hyperic.hq.product.ServerControlPlugin;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPlugin;
import org.hyperic.hq.product.TypeInfo;

import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigOption;
import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.ConfigResponse;

public class JBossServerControlPlugin extends ServerControlPlugin {

    // JBoss server control timeout defaults to 10 minutes
    protected static final int DEFAULT_TIMEOUT = 10 * 60;

    public static final String PROP_CONFIGSET = "configSet";
    static final String PROP_STOP_PROGRAM = "stop.program";
    static final String PROP_STOP_ARGS    = "stop.args";
    static final String PROP_START_ARGS   = "start.args";
    
    private static final String SERVER = "jboss.system:type=Server";

    private String configSet = null;
    private Metric serverMetric = null;
    //flag == true if we are doing stop action using a program
    //rather than JMX
    private boolean isStopProgramAction = false;

    public JBossServerControlPlugin() {
        super();
        //give waitForState enough time
        setTimeout(DEFAULT_TIMEOUT);
    }

    public void configure(ConfigResponse config)
        throws PluginException {

        super.configure(config);

        String val = config.getValue(PROP_CONFIGSET);
        if (val != null) {
            this.configSet = val;
        }
        else {
            getLog().error("Can't find server name in config response " +
                           config);
            throw new PluginException("no server name configured");
        }        
    }

    private Metric configureMetric(String template) {
        String metric = Metric.translate(template, getConfig());

        try {
            return Metric.parse(metric); //parsing will be cached
        } catch (Exception e) {
            getLog().error("Metric parsing error: " + e.getMessage(), e);
            return null;
        }
    }

    private String getConfigSet() {
        return this.configSet;
    }

    private Metric getServerMetric() {
        if (this.serverMetric == null) {
            String metric =
                SERVER + ":Version:" +
                getPluginProperty(JBossMeasurementPlugin.PROP_TEMPLATE_CONFIG);

            this.serverMetric = configureMetric(metric);
        }
        
        return this.serverMetric;
    }

    protected boolean isRunning() {
        try {
            JBossUtil.getMBeanServer(getConfig().toProperties());
            return true;
        } catch (NamingException e) {
            return false;
        } catch (RemoteException e) {
            return false;
        }
    }

    protected File getWorkingDirectory() {
        return new File(getInstallPrefix(), "bin");
    }

    protected boolean isBackgroundCommand() {
        return !this.isStopProgramAction;
    }

    static String getControlScript(boolean isWin32) {
        String sep, ext;

        if (isWin32) {
            sep = "\\";
            ext = "bat";
        }
        else {
            sep = "/";
            ext = "sh";
        }

        return "bin" + sep + "run." + ext;
    }

    protected void getServerConfigSchema(TypeInfo info,
                                         ConfigSchema schema,
                                         ConfigResponse response) {

        boolean isWin32 = getTypeInfo().isWin32Platform();

        //relative to installpath
        setControlProgram(getControlScript(isWin32));

        super.getServerConfigSchema(info, schema, response);

        String installpath =
            response.getValue(ProductPlugin.PROP_INSTALLPATH,
                              getDefaultInstallPath());

        String sep = isWin32 ? "\\" : "/";

        //strip "servers/default"
        //cannot use java.io.File, will convert pathSep
        //if agent is linux and server is win32
        int ix = installpath.lastIndexOf(sep);
        if (ix != -1) {
            String servers = installpath.substring(0, ix);
            final String tok = sep + "servers";
            if (servers.endsWith(tok)) {
                installpath =
                    servers.substring(0, servers.length() - tok.length());
            }
        }

        schema.setDefault(PROP_PROGRAM,
                          installpath + sep + getControlProgram());
    }

    public ConfigSchema getConfigSchema(TypeInfo info, ConfigResponse config) {
        //XXX re-order for UI display
        ConfigSchema schema = super.getConfigSchema(info, config);
        ConfigOption opt = schema.getOption(PROP_PROGRAM);
        opt.setDescription("Server start program");
        List options = schema.getOptions();
        options.remove(opt);
        options.add(0, opt);
        return schema;
    }

    protected String[] getCommandEnv() {
        return new String[] {
            "JBOSS_HOME=" + getJBossHome(),
            "NOPAUSE=true",
        };
    }

    // Check for branded server.
    private boolean isBrandedServer() {
        if (JBossProductPlugin.isBrandedServer(new File(getInstallPrefix()),
                                               getPluginProperty("brand.ear")))
        {
            setResult(RESULT_FAILURE);
            setMessage("Control not supported for " +
                       getPluginProperty("brand.name"));
            return true;
        }
        return false;
    }

    // control methods

    private String[] getProgramArgs(String prop) {
        String cmdline = getConfig(prop);
        if ((cmdline == null) || (cmdline.length() == 0)) {
            return null;
        }
        return StringUtil.explodeQuoted(cmdline);        
    }

    public void start() {
        if (isBrandedServer()) {
            return;
        }

        String[] args = getProgramArgs(PROP_START_ARGS);

        if (args == null) {
            String config = getConfigSet();

            if ((config == null) || (config.length() == 0)) {
                args = new String[0];
            }
            else {
                args = new String[] { "-c", config };
            }
        }

        doCommand(getControlProgram(), args);

        handleResult(STATE_STARTED);
    }

    public void stop() {
        if (isBrandedServer()) {
            return;
        }

        String program = getConfig(PROP_STOP_PROGRAM);
        if ((program == null) || (program.length() == 0)) {
            invokeMethod("shutdown");
            if (getResult() == RESULT_SUCCESS) {
                setMessage("Server stopped via JMX");
            }
        }
        else {
            String cmdline = getConfig(PROP_STOP_ARGS);
            String[] args = StringUtil.explodeQuoted(cmdline);

            this.isStopProgramAction = true;
            try {
                doCommand(program, args);
            } finally {
                //reset for start control
                this.isStopProgramAction = false;
            }
        }

        handleResult(STATE_STOPPED);
    }

    public void restart() {
        if (isBrandedServer()) {
            return;
        }

        boolean hadToStop = false;
        if (isRunning()) {
            hadToStop = true;
            stop();
        }

        if (!hadToStop || (getResult() == RESULT_SUCCESS)) {
            start();
        }
    }

    public void runGarbageCollector() {
        invokeMethod("runGarbageCollector");
    }

    private void invokeMethod(String action) {
        try {
            JBossUtil.invoke(getServerMetric(), action);
            setResult(RESULT_SUCCESS);
        } catch (MetricNotFoundException e) {
            setMessage(e.getMessage());
            setResult(RESULT_FAILURE);
        } catch (MetricUnreachableException e) {
            setMessage(e.getMessage());
            setResult(RESULT_FAILURE);
        } catch (PluginException e) {
            if (action.equals("shutdown") &&
                ! isRunning()) {
                // might get nested java.net.SocketException:
                // Connection reset which is ok/expected-ish if we are
                // stopping the server.
                setResult(RESULT_SUCCESS);
            }
            else {
                setMessage(e.getMessage());
                setResult(RESULT_FAILURE);
            }
        }
    }

    private String getJBossHome() {
        File server = new File(getInstallPrefix());
        File home = server.getParentFile().getParentFile();
        return home.getPath();
    }
}
