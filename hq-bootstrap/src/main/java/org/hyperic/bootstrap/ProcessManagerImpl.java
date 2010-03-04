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
public class ProcessManagerImpl implements ProcessManager  {
    private Sigar sigar;
    private static final int PROCESS_TIMEOUT = 60 * 1000;
    private final Log log = LogFactory.getLog(ProcessManagerImpl.class);

    @Autowired
    public ProcessManagerImpl(Sigar sigar) {
        this.sigar = sigar;
    }

    public long getPidFromPidFile(String pidFile) throws SigarException {
        long[] pids = ProcessFinder.find(sigar, "Pid.PidFile.eq=" + pidFile);
        if (pids.length > 0) {
            return pids[0];
        }
        return -1;
    }

    public void kill(long pid) throws SigarException {
        int signum = Sigar.getSigNum("TERM");
        sigar.kill(pid, signum);
    }

    public int executeProcess(String[] commandLine, String workingDir, boolean suppressOutput) {
        return executeProcess(commandLine, workingDir, null, suppressOutput);
    }

    public int executeProcess(String[] commandLine, String workingDir, String[] envVariables,
                              boolean suppressOutput) {
        ExecuteWatchdog watchdog = new ExecuteWatchdog(ProcessManagerImpl.PROCESS_TIMEOUT);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        Execute ex = new Execute(new PumpStreamHandler(output), watchdog);
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
        if (!suppressOutput) {
            String message = output.toString();

            if (message.length() > 0 && exitCode != 0) {
                log.error(message);
            } else if (message.length() > 0) {
                log.info(message);
            }
        }

        if (watchdog.killedProcess()) {
            String err = "Command did not complete within timeout of " +
                         ProcessManagerImpl.PROCESS_TIMEOUT / 1000 + " seconds";
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
}
