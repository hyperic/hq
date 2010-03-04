package org.hyperic.bootstrap;

import org.hyperic.sigar.SigarException;

/**
 * Platform-independent component responsible for process execution and
 * obtaining process status
 * @author jhickey
 * 
 */
public interface ProcessManager {

    /**
     * 
     * @param pidFile The pidfile of the process to lookup
     * @return the PID of the process from the file ONLY if the process is
     *         running and the file is found. Else returns -1.
     * @throws SigarException If an error occurs querying for the process
     */
    long getPidFromPidFile(String pidFile) throws SigarException;

    /**
     * Performs a process kill(TERM)
     * @param pid The PID of the process to kill
     * @throws SigarException
     */
    void kill(long pid) throws SigarException;

    /**
     * Executes a process, blocking until process execution completes or a
     * timeout is reached.
     * @param commandLine The command line of the process to execute
     * @param workingDir The working dir from which to execute the process
     * @param suppressOutput If true, process out and process err will not be
     *        printed. If false, process out will print to log.info and process
     *        error will print to log.err
     * @return The exit code of the process
     */
    int executeProcess(String[] commandLine, String workingDir, boolean suppressOutput);

    /**
     * Executes a process, blocking until process execution completes or a
     * timeout is reached.
     * @param commandLine The command line of the process to execute
     * @param workingDir The working dir from which to execute the process
     * @param envVariables The process environment
     * @param suppressOutput If true, process out and process err will not be
     *        printed. If false, process out will print to log.info and process
     *        error will print to log.err
     * @return The exit code of the process
     */
    int executeProcess(String[] commandLine, String workingDir, String[] envVariables,
                       boolean suppressOutput);

    /**
     * Checks if a port is bound. Will sleep 2 seconds and continue trying until
     * port is bound or maxTries has been reached
     * @param port The port to check
     * @param maxTries The number of times to check if the port is not bound
     * @return true if port is bound sometime w/in maxTries * 2 seconds, false
     *         if not bound
     * @throws Exception
     */
    boolean isPortInUse(long port, int maxTries) throws Exception;

}