/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 * 
 * Copyright (C) [2004-2013], Hyperic, Inc.
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.logging.Log;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.product.ControlPlugin;
import static org.hyperic.hq.product.ControlPlugin.RESULT_FAILURE;
import static org.hyperic.hq.product.ControlPlugin.RESULT_SUCCESS;
import static org.hyperic.hq.product.ControlPlugin.STATE_STARTED;
import static org.hyperic.hq.product.GenericPlugin.getScriptExtension;

import org.hyperic.sigar.OperatingSystem;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.sigar.win32.Win32;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;

public class JBoss7Control extends ControlPlugin {

    private final Log log = getLog();
    private JBossAdminHttp admin;
    private static final String BACKGROUND_SCRIPT = "background" + getScriptExtension();
    private String script;
    private String cwd;
    private String background;
    private List<String> prefix;
    private List<String> args;
    public static String START_SCRIPT = "jboss7.start.script";
    private String PREFIX = "jboss7.start.prefix";
    private String ARGS = "jboss7.start.args";

    @Override
    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);
        log.debug("[configure] config=" + config);
        admin = new JBossAdminHttp(getConfig().toProperties());

        // start script
        File sh = new File(getConfig(START_SCRIPT));
        if (sh.exists()) {
            cwd = sh.getParentFile().getAbsolutePath();
            script = sh.getAbsolutePath();
        } else {
            throw new PluginException("Start script '" + sh + "' NOT FOUND");
        }

        // parse args
        String argsStr = getConfig(ARGS);
        args = new ArrayList<String>();
        if (argsStr != null && argsStr.length() != 0) {
            try {
                String[] argsArgs = StringUtil.explodeQuoted(argsStr);
                args.addAll(Arrays.asList(argsArgs));
            } catch (IllegalArgumentException e) {
                throw new PluginException("Unable to parse arguments: '" + argsStr + "' " + e, e);
            }
        }

        // parse prefix
        String prefixStr = getConfig(PREFIX);
        prefix = new ArrayList<String>();
        if (prefixStr != null && prefixStr.length() != 0) {
            try {
                String[] prefixArgs = StringUtil.explodeQuoted(prefixStr);
                prefix.addAll(Arrays.asList(prefixArgs));
            } catch (IllegalArgumentException e) {
                throw new PluginException("Unable to parse prefix: '" + prefixStr + "' " + e, e);
            }
        }


        File agentBundleHome = new File(System.getProperty(AgentConfig.AGENT_BUNDLE_HOME));
        File bg = new File(agentBundleHome, BACKGROUND_SCRIPT);
        log.debug("[configure] agentBundleHome =" + agentBundleHome.getAbsolutePath());
        log.debug("[configure] bg =" + bg.getAbsolutePath());
        if (bg.exists()) {
            try {
                background = bg.getCanonicalPath();
            } catch (IOException ex) {
                background = bg.getAbsolutePath();
            }
        } else {
            throw new PluginException("Background script '" + bg + "' NOT FOUND");
        }

        log.debug("[configure] background =" + background);
        log.debug("[configure] cwd = " + cwd);
        log.debug("[configure] prefix = " + prefix);
        log.debug("[configure] script = " + script);
        log.debug("[configure] args = " + args);
    }

    @Override
    public List<String> getActions() {
        return Arrays.asList("start", "stop", "restart");
    }

    public void stop() throws PluginException {
        log.debug("[stop] config=" + getConfig());
        if (!isRunning()) {
            throw new PluginException("Server is not running");
        }
        admin.shutdown();
        waitForState(STATE_STOPPED);
    }

    @Override
    protected boolean isRunning() {
        boolean res = true;
        try {
            admin.testConnection();
        } catch (PluginException ex) {
            log.debug(ex, ex);
            res = false;
        }
        log.debug("[isRunning] res = " + res);
        return res;
    }

    public void start() throws PluginException {
        log.debug("[start] config=" + getConfig());
        if (isRunning()) {
            throw new PluginException("Server is running");
        }
        doCommand();
        waitForState(STATE_STARTED);
    }

    public void restart() throws PluginException {
        log.debug("[restart]");
        if (isRunning()) {
            stop();
        }
        start();
    }

    protected void doCommand() throws PluginException {
        List<String> cmd = new ArrayList<String>();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(getTimeoutMillis());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Execute ex = new Execute(new PumpStreamHandler(output), watchdog);
        ex.setWorkingDirectory(new File(cwd));

        cmd.addAll(prefix);
        cmd.add(background);

        if (OperatingSystem.IS_WIN32) {
            //Runtime.exec does not handle file associations
            //such as foo.pl -> perl.exe, foo.py -> python.exe.
            String exe = Win32.findScriptExecutable(script);
            if (exe != null) {
                cmd.add(exe);
            }
        } else {
            cmd.add(script);
        }

        cmd.addAll(args);

        log.debug("[doCommand] cmd=" + cmd);

        ex.setCommandline((String[]) cmd.toArray(new String[0]));

        String[] env = {"HQ_CONTROL_WAIT=" + 10};

        env = (String[]) ArrayUtil.combine(env, ex.getEnvironment());

        ex.setEnvironment(env);
        int exitCode = RESULT_FAILURE;


        try {
            exitCode = ex.execute();
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
            throw new PluginException(e);
        }
        
        if (exitCode == 0) {
            setResult(RESULT_SUCCESS);
            setMessage("OK");
        } else {
            setResult(RESULT_FAILURE);
            setMessage(output.toString());
        }

        // Check for watchdog timeout.  Note this does not work with scripts
        // that are backgrounded.
        if (watchdog.killedProcess()) {
            String err = "Command did not complete within timeout of " + getTimeout() + " seconds";
            getLog().error(err);
            setMessage(err);
            setResult(RESULT_FAILURE);
        }

        log.debug("[doCommand] result=" + getResult() + ", exitCode=" + exitCode);
        if (getResult() == RESULT_FAILURE) {
            throw new PluginException(getMessage());
        }
    }
}
