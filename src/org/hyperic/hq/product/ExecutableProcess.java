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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hyperic.sigar.OperatingSystem;
import org.hyperic.sigar.win32.Win32;
import org.hyperic.util.StringUtil;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;

public class ExecutableProcess extends Collector {
    public static final String DOMAIN = "exec";

    public static final String PROP_EXEC = "exec";
    public static final String PROP_FILE = "file";
    public static final String PROP_ARGS = "args";
    private static Log log =
        LogFactory.getLog(ExecutableProcess.class.getName());

    private String[] argv;
    private File file;
    private File cwd;
    private ByteArrayOutputStream output = new ByteArrayOutputStream();

    protected int getDefaultTimeout() {
        return 2 * 60; //2 minutes
    }

    protected int getLogLevel(int rc) {
        switch (rc) {
            case 0:
                return LogTrackPlugin.LOGLEVEL_INFO;
            case 1:
                return LogTrackPlugin.LOGLEVEL_WARN;
            case 2:
            case 3:
            default:
                return LogTrackPlugin.LOGLEVEL_ERROR;
        }
    }

    protected double getAvailValue(int rc) {
        switch (rc) {
            case 0:
                return Metric.AVAIL_UP;      //green light
            case 1:
                return Metric.AVAIL_WARN;    //yellow light
            case 2:
                return Metric.AVAIL_DOWN;    //red light
            case 3:
                return Metric.AVAIL_UNKNOWN; //grey light
            case 4:
                return Metric.AVAIL_PAUSED;  //orange light
            default:
                return Metric.AVAIL_UNKNOWN;
        }        
    }
    
    //this method modifies the List argv param (sorry jav)
    private void addArgv(String value, ArrayList argv) {
        if ((value != null) && (value.length() != 0)) {
            String[] add = StringUtil.explodeQuoted(value);
            for (int i=0; i<add.length; i++) {
                argv.add(add[i]);
            }
        }
    }
    
    protected String getExecProperty() {
        return getCollectorProperty(PROP_EXEC);
    }
    
    protected String getFileProperty() {
        return getCollectorProperty(PROP_FILE);
    }
    
    protected String getArgsProperty() {
        return getCollectorProperty(PROP_ARGS);
    }
    
    protected void init() {
        String exec = getExecProperty();
        String name = getFileProperty();

        ArrayList argv = new ArrayList();

        if (OperatingSystem.IS_WIN32 && (exec == null)) {
            //Runtime.exec does not handle file associations
            //such as foo.pl -> perl.exe, foo.py -> python.exe.
            String exe = Win32.findScriptExecutable(name);
            if (exe != null) {
                argv.add(exe);
                if (exe.endsWith("cscript.exe")) {
                    //surpress banner for .vbs scripts
                    argv.add("//nologo");
                }
            }
        }

        addArgv(exec, argv);

        argv.add(name);
        
        addArgv(getArgsProperty(), argv);

        this.argv = new String[argv.size()];
        argv.toArray(this.argv);

        setSource(name);

        if (name == null) {
            String msg =
                "No file in properties: " +
                getProperties();
            throw new IllegalArgumentException(msg);
        }

        this.file = new File(name);
        if (!this.file.exists()) {
            String msg =
                "File '" + this.file + "' does not exist";
            throw new IllegalArgumentException(msg);
        }
        if (this.file.isAbsolute()) {
            this.cwd = this.file.getParentFile();
        }
        else {
            this.cwd = new File(".");
        }
    }

    public boolean isPoolable() {
        return true;
    }

    public void collect() {
        this.output.reset();

        ExecuteWatchdog wdog =
            new ExecuteWatchdog(getTimeoutMillis());
        Execute exec =
            new Execute(new PumpStreamHandler(this.output), wdog);

        exec.setCommandline(this.argv);

        exec.setWorkingDirectory(this.cwd);

        if (log.isDebugEnabled()) {
            log.debug(" running: " + this);
        }

        int res;
        
        try {
            startTime();
            res = exec.execute();
            endTime();
            String outputMessage = this.output.toString(); 

            if ((outputMessage == null) ||
                (outputMessage.length() == 0))
            {
                outputMessage = "No message, exit value=" + res;
            }
            setMessage(outputMessage);
            parseResults(getMessage());
        } catch (Exception e) {
            if (log.isDebugEnabled()) {
                log.error(this + ": " + e.getMessage(), e);
            }

            setMessage(e.toString());
            if (!this.file.exists()) {
                setMessage("File '" + this.file +
                           "' does not exist");
            }
            res = 2;
        }
        
        if (wdog.killedProcess()) {
            setMessage("Timeout running " +
                       "[" + exec.getCommandLineString() + "]");
        }

        setLogLevel(getLogLevel(res));
        double avail = getAvailValue(res);
        String msg = this + ": " + getMessage();

        switch (getLogLevel()) {
            case LogTrackPlugin.LOGLEVEL_ERROR:
                log.error(msg);
                break;
            case LogTrackPlugin.LOGLEVEL_WARN:
                log.warn(msg);
                break;
            case LogTrackPlugin.LOGLEVEL_INFO:
            case LogTrackPlugin.LOGLEVEL_DEBUG:
            default:
                log.debug(msg);
                break;
        }

        setAvailability(avail);
        setResponseCode(res);
    }
    
    public String toString() {
        return Arrays.asList(this.argv).toString();
    }
}
