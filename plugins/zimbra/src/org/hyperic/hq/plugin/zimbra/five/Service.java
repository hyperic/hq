/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.zimbra.five;

/**
 *
 * @author laullon
 */
public class Service {

    private String name, pidFile, log, process;

    public Service(String name, String pidFile, String log, String process) {
        this.name = name;
        this.pidFile = pidFile;
        this.log = log;
        this.process = process;
    }

    public Service(String name, String pidFile, String log) {
        this.name = name;
        this.pidFile = pidFile;
        this.log = log;
    }

    public Service(String name, String pidFile) {
        this.name = name;
        this.pidFile = pidFile;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the pidFile
     */
    public String getPidFile() {
        return pidFile;
    }

    /**
     * @return the log
     */
    public String getLog() {
        return log;
    }

    /**
     * @return the process
     */
    public String getProcess() {
        return process;
    }
}
