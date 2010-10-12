/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 *  "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */

package org.hyperic.bootstrap;

import java.io.ByteArrayOutputStream;
import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarFileNotFoundException;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.hyperic.util.exec.Execute;
import org.hyperic.util.exec.ExecuteWatchdog;
import org.hyperic.util.exec.PumpStreamHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Default implementation of {@link ProcessManager}
 * @author jhickey
 * 
 */
@Component
public class ProcessManagerImpl implements ProcessManager {
    private Sigar sigar;
    private final Log log = LogFactory.getLog(ProcessManagerImpl.class);

    @Autowired
    public ProcessManagerImpl(Sigar sigar) {
        this.sigar = sigar;
    }

    public long getPidFromPidFile(String pidFile) throws SigarException {
        return getPidFromProcQuery("Pid.PidFile.eq=" + pidFile);
    }

    public long getPidFromProcQuery(String ptql) throws SigarException {
        long[] pids = ProcessFinder.find(sigar, ptql);
        if (pids.length > 0) {
            return pids[0];
        }
        return -1;
    }

    public void kill(long pid) throws SigarException {
        int signum = Sigar.getSigNum("TERM");
        sigar.kill(pid, signum);
    }

    public void forceKill(long pid) throws SigarException {
        int signum = Sigar.getSigNum("KILL");
        sigar.kill(pid, signum);
    }

    public int executeProcess(String[] commandLine, String workingDir, boolean suppressOutput,
                              int timeout) {
        log.debug("Command line: " +commandLine );
        return executeProcess(commandLine, workingDir, null, suppressOutput, timeout);
    }

    public int executeProcess(String[] commandLine, String workingDir, String[] envVariables,
                              boolean suppressOutput, int timeout) {

        ExecuteWatchdog watchdog = null;
        if (timeout != -1) {
            watchdog = new ExecuteWatchdog(timeout);
        }

        Execute ex = null;
        if (suppressOutput) {
            ex = new Execute(new PumpStreamHandler(new ByteArrayOutputStream()), watchdog);
        } else {
            // send standard output and error of subprocesses to standard 
            // output and error of the parent process
            ex = new Execute(new PumpStreamHandler(System.out, System.err), watchdog);
        }
        ex.setWorkingDirectory(new File(workingDir));
        ex.setCommandline(commandLine);
        if (envVariables != null) {
            ex.setEnvironment(envVariables);
        }
        int exitCode = 0;
        try {
            exitCode = ex.execute();
        } catch (Exception e) {
            exitCode = 1;
            log.error(e.getMessage(), e);
        }
        // Don't log as error if exit code is 143 - will happen if process
        // is terminated by kill
        if (exitCode != 0 && exitCode != 143) {
            String err = "Command did not execute successfully (exit code = " + exitCode + ")";
            log.error(err);
        } else {
            String info = "Command executed successfully (exit code = " + exitCode + ") ";
            log.info(info);
        }
        if (watchdog != null && watchdog.killedProcess()) {
            String err = "Command did not complete within timeout of " +
                         timeout + " seconds";
            log.error(err);
        }
        return exitCode;
    }

    public boolean isPortInUse(long port, int maxTries) throws Exception {
        log.debug("waitForPort " + port + ", entering wait loop: MAXTRIES=" + maxTries);
        for (int i = 0; i < maxTries; i++) {
            log.debug("checking port: " + port + "...");
            try {
                if (sigar.getNetListenAddress(port) != null) {
                    // we were able to find something
                    return true;
                }
            } catch (SigarFileNotFoundException e) {
                // means port is not bound
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        log.debug("Num tries for port check exhausted");
        return false;
    }

    public boolean waitForProcessDeath(int maxTries, long pid) throws Exception {
        for (int i = 0; i < maxTries; i++) {
            log.debug("waitForPid: waiting");
            pid = getPidFromProcQuery("Pid.Pid.eq=" + pid);
            if (pid == -1) {
                log.info("HQ server exited");
                return true;
            }
            log.debug("waitForPid: PID " + pid + " still alive");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        log.debug("Num tries for server PID check exhausted");
        log.info("HQ server PID " + pid + " did not exit.");
        return false;
    }
}
