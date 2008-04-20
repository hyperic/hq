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

package org.hyperic.hq.product;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Properties;

import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.win32.Win32;

import org.hyperic.util.config.ConfigSchema;
import org.hyperic.util.config.ConfigResponse;
import org.hyperic.util.config.StringConfigOption;
import org.hyperic.util.config.IntegerConfigOption;

import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;

import org.hyperic.util.ArrayUtil;
import org.hyperic.util.StringUtil;

/**
 * This class is mainly helpful for control plugins which are
 * script/process driven.
 */
public abstract class ServerControlPlugin extends ControlPlugin {

    public static final String PROP_PIDFILE       = "pidfile";
    public static final String PROP_PROGRAM       = "program";
    public static final String PROP_PROGRAMPREFIX = "prefix";

    private static final String PROGRAM_PREFIX    = "";

    private ByteArrayOutputStream output = new ByteArrayOutputStream();

    private String installPrefix = null;
    private String controlProgram = null;
    private String controlProgramPrefix = null;
    private String pidFile = null;
    private int backgroundWaitTime = 0;

    private Sigar sigar = null;

    //assumes we are running on the agent; which is ok since the control
    //files we exec are too.
    private static final String BACKGROUND_SCRIPT =
        "background" + getScriptExtension();

    private int exitCode;

    public ServerControlPlugin() {
        super();
        setControlProgramPrefix(PROGRAM_PREFIX);
    }

    public String getInstallPrefix() {
        return this.installPrefix;
    }

    public void setInstallPrefix(String val) {
        this.installPrefix = val;
    }

    public String getControlProgram() {
        if (this.controlProgram == null) {
            //UI default for xml-only plugin
            return getTypeProperty("DEFAULT_PROGRAM");
        }
        else {
            return this.controlProgram;
        }
    }

    public void setControlProgram(String val) {
        this.controlProgram = val;
    }

    public String getControlProgramPrefix() {
        return this.controlProgramPrefix;
    }

    public void setControlProgramPrefix(String val) {
        this.controlProgramPrefix = val;
    }

    public String getPidFile() {
        return this.pidFile;
    }

    public void setPidFile(String val) {
        this.pidFile = val;
    }

    protected boolean useSigar() {
        return false;
    }

    public void configure(ConfigResponse config)
        throws PluginException
    {
        super.configure(config);

        String val;

        val = config.getValue(ControlPlugin.PROP_TIMEOUT);
        if (val != null) {
            setTimeout(val);
        }

        val = config.getValue(ProductPlugin.PROP_INSTALLPATH);
        if (val != null) {
            setInstallPrefix(val);
        }

        val = config.getValue(PROP_PROGRAM);
        if (val != null) {
            setControlProgram(val);
        }

        val = config.getValue(PROP_PROGRAMPREFIX);
        if (val != null) {
            try {
                setControlProgramPrefix(val);
            } catch (IllegalArgumentException e) {
                // Unable to parse args, probably due to mismatched quotes
                throw new PluginException("Unable to parse prefix arguments");
            }
        }

        val = config.getValue(PROP_PIDFILE);
        if (val != null) {
            setPidFile(val);
        }
        
        if (useSigar()) {
            this.sigar = new Sigar();
        }
    }

    public void shutdown() throws PluginException {
        super.shutdown();
        if (this.sigar != null) {
            this.sigar.close();
        }
    }

    protected void getServerConfigSchema(TypeInfo info,
                                         ConfigSchema schema,
                                         ConfigResponse response) {

        //getConfigSchema happens server-side; so we cant use File.separator
        String fileSep = info.isWin32Platform() ? "\\" : "/";
        String entName = info.getName();
        StringConfigOption opt;

        String installPrefix =
            response.getValue(ProductPlugin.PROP_INSTALLPATH,
                              getInstallPrefix());
        if (installPrefix == null) {
            installPrefix = getDefaultInstallPath();
        }

        String pidFile = getPidFile();

        if (pidFile != null) {
            opt = new StringConfigOption(PROP_PIDFILE,
                                         "Full path to " + entName +
                                         " pid file",
                                         installPrefix + 
                                         fileSep +
                                         pidFile);
            schema.addOption(opt);
        }

        String controlProgram = getControlProgram();

        if (controlProgram != null) {
            opt = new StringConfigOption(PROP_PROGRAM,
                                         "Full path to " + entName +
                                         " control program",
                                         installPrefix +
                                         fileSep +
                                         controlProgram);
            schema.addOption(opt);
        }

        String controlProgramPrefix = getControlProgramPrefix();
        
        if (controlProgramPrefix != null) {
            opt = new StringConfigOption(PROP_PROGRAMPREFIX,
                                         "Prefix arguments to control " +
                                         "program", controlProgramPrefix);
            opt.setOptional(true);
            schema.addOption(opt);
        }

        IntegerConfigOption timeout = 
             new IntegerConfigOption(ControlPlugin.PROP_TIMEOUT,
                                     "Timeout of control operations " +
                                     "(in seconds)",
                                     new Integer(getTimeout()));
        schema.addOption(timeout);
    }

    //override to ask for more
    public ConfigSchema getConfigSchema(TypeInfo info,
                                        ConfigResponse config)
    {
        ConfigSchema schema = super.getConfigSchema(info, config);

        if (useConfigSchema(info)) {
            getServerConfigSchema(info, schema, config);
        }

        return schema;
    }

    protected boolean useConfigSchema(TypeInfo info) {
        return info.getType() == TypeInfo.TYPE_SERVER;
    }

    //override for to use something other than pid file
    protected boolean isRunning() {
        return isProcessRunning(getPidFile());
    }

    protected boolean isProcessRunning(int pid) {
        if (this.sigar == null) {
            return false;
        }

        try {
            ProcState ps = this.sigar.getProcState(pid);
            
            // XXX: check process state?
            return true;
        } catch (SigarException e) {
        }

        return false;
    }

    protected boolean isProcessRunning(String pidFile) {
        String pid;

        if (pidFile == null) {
            return false;
        }

        // Get the process id using the pid file.  It is assumed that
        // the pidfile contains a single line with the process if of
        // the server process.

        try {
            BufferedReader in =
                new BufferedReader(new FileReader(pidFile));
            pid = in.readLine();
        } catch (FileNotFoundException e) {
            // If the pid file does not exist, we are not running
            String err = "Pid file: " + pidFile + " not found";
            getLog().debug(err);
            return false;
        } catch (IOException e) {
            // XXX is it right to Assume we are running?
            getLog().info("Could not read pidFile=" + pidFile);
            return true;
        }

        int processId;
        try {
            processId = Integer.parseInt(pid);
        } catch (NumberFormatException e) {
            String err = "Failed to parse pid from pid file: " + pidFile;
            getLog().debug(err);
            setMessage(err);
            return false;
        }

        return isProcessRunning(processId);
    }

    protected File getWorkingDirectory() {
        File file = new File(getControlProgram()).getParentFile();
        if (file == null || !file.isAbsolute()) {
            file = new File(installPrefix);
        }
        return file;
    }

    protected String getControlProgramDir() {
        return new File(getControlProgram()).getParent();
    }

    protected void validateControlProgram(String name)
        throws PluginException {

        String pgm = getControlProgram();

        File script = new File(pgm);

        if (!script.exists()) {
            String msg = name + " control program not found: " + pgm;
            throw new PluginException(msg);
        }
    }

    protected boolean isBackgroundCommand() {
        return false;
    }

    /**
     * @return Seconds to wait on a background process
     */
    protected int getBackgroundWaitTime() {
        String time = getPluginProperty("CONTROL_WAIT_TIME");
        if (time != null) {
            return Integer.parseInt(time);
        }
        else {
            return this.backgroundWaitTime;
        }
    }

    protected void setBackgroundWaitTime(int seconds) {
        this.backgroundWaitTime = seconds;
    }

    /**
     * Override to add any additional arguments to the command line.
     */
    protected String[] getCommandArgs() {
        return new String[0];
    }

    /**
     * Override to pass any addition environment variables to the command.
     */
    protected String[] getCommandEnv() {
        return null;
    }
    
    protected int doCommand() {
        return doCommand(getControlProgram(), new String[0]);
    }
    
    protected int doCommand(String command) {
        if (command == null) {
            return doCommand();
        }
        else {
            return doCommand(new String[] { command });
        }
    }

    protected int doCommand(String[] args) {
        return doCommand(getControlProgram(), args);
    }
    
    protected int doCommand(String program, String arg) {
        return doCommand(program, new String[] { arg });
    }

    private String[] combine(String[] a1, String[] a2) {
        return (String[])ArrayUtil.combine(a1, a2);    
    }

    protected int doCommand(String program, String[] params)
    {
        ArrayList args = new ArrayList();
        ExecuteWatchdog watchdog = 
            new ExecuteWatchdog(getTimeoutMillis());

        this.output.reset();

        Execute ex = new Execute(new PumpStreamHandler(this.output),
                                 watchdog);

        //weblogic and jboss scripts for example
        //must be run from the directory where the script lives
        File wd = getWorkingDirectory();
        if (wd.exists()) {
            ex.setWorkingDirectory(wd);
        }

        if (isBackgroundCommand()) {
            //XXX bloody ugly hack for startup scripts such as weblogic
            //which do not background themselves
            Properties props = getManager().getProperties(); 
            String cwd = System.getProperty("user.dir");
            String dir = props.getProperty("agent.install.home", cwd);

            if (dir.equals(".")) {
                //XXX: ./background.sh command silently fails when running
                //in the agent, even tho ./ is the same place as user.dir
                dir = cwd; 
            }

            File background = new File(dir, BACKGROUND_SCRIPT);
            if (!background.exists()) {
                //try relative to pdk.dir for command-line usage
                String pdk = ProductPluginManager.getPdkDir();
                dir = pdk + "/../";
                background = new File(dir, BACKGROUND_SCRIPT);
            }
            if (background.exists()) {
                args.add(background.toString());
            }
        }

        String prefix = getControlProgramPrefix();
        if (prefix != null && prefix.length() != 0) {
            try {
                String[] prefixArgs = 
                    StringUtil.explodeQuoted(getControlProgramPrefix());

                for (int i=0; i<prefixArgs.length; i++) {
                    args.add(prefixArgs[i]);
                }
            } catch (IllegalArgumentException e) {
                // Unable to parse args, probably due to mismatched quotes
                getLog().error("Unable to parse arguments: " + prefix);
            }
        }

        if (OperatingSystem.IS_WIN32) {
            //Runtime.exec does not handle file associations
            //such as foo.pl -> perl.exe, foo.py -> python.exe.
            String exe = Win32.findScriptExecutable(program);
            if (exe != null) {
                args.add(exe);
            }
        }

        if (new File(program).isAbsolute())
            args.add(program);
        else
            args.add(installPrefix+File.separator+program);

        if (params != null) {
            for (int i=0; i<params.length; i++) {
                if (params[i] == null) {
                    continue;
                }
                args.add(params[i]);
            }
        }

        String[] commandArgs = getCommandArgs();

        for (int i=0; i<commandArgs.length; i++) {
            if (commandArgs[i] == null) {
                continue;
            }
            args.add(commandArgs[i]);
        }

        getLog().info("doCommand args=" + args);

        ex.setCommandline((String[])args.toArray(new String[0]));

        //set some environment variables for use by control scripts
        String[] env = {
            "HQ_CONTROL_RESOURCE=" + getName(),
            "HQ_CONTROL_TYPE=" + getTypeInfo().getName(),
            "HQ_CONTROL_WAIT=" + getBackgroundWaitTime(),
        };

        env = combine(env, ex.getEnvironment());
        String[] cmdEnv = getCommandEnv();
        if (cmdEnv != null) {
            env = combine(env, cmdEnv);
        }

        ex.setEnvironment(env);

        this.exitCode = RESULT_FAILURE;

        try {
            this.exitCode = ex.execute();
        } catch (Exception e) {
            getLog().error(e.getMessage(), e);
            setMessage(e.getMessage());
        }

        if (this.exitCode == 0) {
            setResult(RESULT_SUCCESS);
        }
        else {
            setResult(RESULT_FAILURE);
        }

        // Check for watchdog timeout.  Note this does not work with scripts
        // that are backgrounded.
        if (watchdog.killedProcess()) {
            String err = "Command did not complete within timeout of " +
                getTimeout() + " seconds";
            getLog().error(err);
            setMessage(err);
            setResult(RESULT_FAILURE);
            return getResult();
        }

        getLog().info("doCommand result=" + getResult() +
                      ", exitCode=" + this.exitCode);

        String message = this.output.toString();
        if (message.length() > 0) {
            setMessage(message);
        }
        return getResult();
    }

    /**
     * @return program exit code from doCommand()
     */
    protected int getExitCode() {
        return this.exitCode;
    }

    protected void handleResult(String stateWanted) {
        // don't bother waiting for the desired state if the startup
        // script does not return 0.

        if (getResult() != RESULT_SUCCESS) {
            if (getMessage() == null) {
                setMessage("Unknown Error (exit code=" +
                           this.exitCode + ")");
            }

            return;
        }

        String state = waitForState(stateWanted);

        if (!state.equals(stateWanted)) {
            setResult(RESULT_FAILURE);

            if (getMessage() == null) {
                setMessage("Control action timed out after " + getTimeout() +
                           " seconds.  Server still in state " + state);
            }
        }
    }
    
    protected int start(String command) {
        int res = doCommand(command);

        waitForState(STATE_STARTED);

        return res;
    }
}
