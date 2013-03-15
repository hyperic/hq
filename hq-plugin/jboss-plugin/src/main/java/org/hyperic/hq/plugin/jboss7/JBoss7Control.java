/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.jboss7;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.hyperic.hq.agent.AgentConfig;
import org.hyperic.hq.product.ControlPlugin;
import static org.hyperic.hq.product.ControlPlugin.RESULT_FAILURE;
import static org.hyperic.hq.product.ControlPlugin.RESULT_SUCCESS;
import static org.hyperic.hq.product.GenericPlugin.getScriptExtension;
import org.hyperic.hq.product.PluginException;
import org.hyperic.hq.product.ProductPluginManager;
import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.win32.Win32;
import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;

/**
 *
 * @author laullon
 */
public class JBoss7Control extends ControlPlugin {

    private final Log log = getLog();
    private JBossAdminHttp admin;
    private static final String BACKGROUND_SCRIPT = "background" + getScriptExtension();
    private String script;
    private String cwd;
    private String background;
    private List<String> prefix;
    public static String START_SCRIPT = "jboss7.start.script";
    private String PREFIX = "jboss7.start.prefix";

    @Override
    public void configure(ConfigResponse config) throws PluginException {
        super.configure(config);
        log.debug("[__configure] config=" + config);
        admin = new JBossAdminHttp(getConfig().toProperties());

        // start script
        File sh = new File(getConfig(START_SCRIPT));
        if (sh.exists()) {
            cwd = sh.getParentFile().getAbsolutePath();
            script = sh.getAbsolutePath();
        } else {
            throw new PluginException("Start script '" + sh + "' NOT FOUND");
        }

        // parse prefix
        String prefixStr = getConfig(PREFIX);
        prefix = new ArrayList<String>();
        if (prefixStr != null && prefixStr.length() != 0) {
            try {
                String[] prefixArgs = StringUtil.explodeQuoted(prefixStr);
                for (int i = 0; i < prefixArgs.length; i++) {
                    prefix.add(prefixArgs[i]);
                }
            } catch (IllegalArgumentException e) {
                throw new PluginException("Unable to parse arguments: '" + prefix + "' " + e, e);
            }
        }

        // find BACKGROUND_SCRIPT
        Properties props = getManager().getProperties();
        String cwd = System.getProperty("user.dir");
        String dir = "";
        try {
            dir = props.getProperty(AgentConfig.PROP_INSTALLHOME[0]);
        } catch (java.lang.NoClassDefFoundError e) { // in standalone -> Exception in thread "main" java.lang.NoClassDefFoundError: org/hyperic/hq/agent/AgentConfig
            dir = props.getProperty("agent.install.home", cwd);
        }

        File bg = new File(dir, BACKGROUND_SCRIPT);
        if (!bg.exists()) {
            //try relative to pdk.dir for command-line usage
            File pdk = new File(ProductPluginManager.getPdkDir());
            if (!pdk.isAbsolute()) {
                pdk = new File(new File(cwd), ProductPluginManager.getPdkDir());
            }
            bg = new File(pdk, "../" + BACKGROUND_SCRIPT);
        }
        if (bg.exists()) {
            background = bg.getAbsolutePath();
        } else {
            throw new PluginException("Background script '" + bg + "' NOT FOUND");
        }

        log.debug("[configure] background =" + background);
        log.debug("[configure] cwd = " + cwd);
        log.debug("[configure] script = " + script);
    }

    @Override
    public List<String> getActions() {
        return Arrays.asList("start", "stop");
    }

    public void stop() throws PluginException {
        log.debug("[stop] config=" + getConfig());
        if (!isRunning()) {
            throw new PluginException("Server is not running");
        }
        admin.shutdown();
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
        if (isRunning()) {
            throw new PluginException("Server is running");
        }
        doCommand();
        waitForState(STATE_STARTED);
    }

    protected void doCommand() {
        List<String> args = new ArrayList<String>();
        ExecuteWatchdog watchdog = new ExecuteWatchdog(getTimeoutMillis());

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Execute ex = new Execute(new PumpStreamHandler(output), watchdog);
        ex.setWorkingDirectory(new File(cwd));

        args.addAll(prefix);
        args.add(background);

        if (OperatingSystem.IS_WIN32) {
            //Runtime.exec does not handle file associations
            //such as foo.pl -> perl.exe, foo.py -> python.exe.
            String exe = Win32.findScriptExecutable(script);
            if (exe != null) {
                args.add(exe);
            }
        } else {
            args.add(script);
        }

        log.debug("[doCommand] args=" + args);

        ex.setCommandline((String[]) args.toArray(new String[0]));

        String[] env = {"HQ_CONTROL_WAIT=" + 10};

        env = (String[]) ArrayUtil.combine(env, ex.getEnvironment());

        ex.setEnvironment(env);
        int exitCode = RESULT_FAILURE;


        try {
            exitCode = ex.execute();
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
            setMessage(e.getMessage());
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
    }
}
