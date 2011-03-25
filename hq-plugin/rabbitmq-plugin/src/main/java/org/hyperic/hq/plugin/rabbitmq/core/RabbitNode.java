/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hyperic.hq.plugin.rabbitmq.core;

import java.util.List;
import java.util.Map;

/**
 *
 * @author administrator
 */
public class RabbitNode {

    private String name;
    private boolean running;
    private String type;
    private int osPid;
    private int memEts;
    private int memBinary;
    private int fdUsed;
    private int fdTotal;
    private int memUsed;
    private int memLimit;
    private int procUsed;
    private int procTotal;
    private String erlangVersion;
    private int uptime;
    private int runQueue;
    private int processors;
    private String statisticsLevel;
    private List<Map<String, String>> applications;

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @param running the running to set
     */
    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the osPid
     */
    public int getOsPid() {
        return osPid;
    }

    /**
     * @param osPid the osPid to set
     */
    public void setOsPid(int osPid) {
        this.osPid = osPid;
    }

    /**
     * @return the memEts
     */
    public int getMemEts() {
        return memEts;
    }

    /**
     * @param memEts the memEts to set
     */
    public void setMemEts(int memEts) {
        this.memEts = memEts;
    }

    /**
     * @return the memBinary
     */
    public int getMemBinary() {
        return memBinary;
    }

    /**
     * @param memBinary the memBinary to set
     */
    public void setMemBinary(int memBinary) {
        this.memBinary = memBinary;
    }

    /**
     * @return the fdUsed
     */
    public int getFdUsed() {
        return fdUsed;
    }

    /**
     * @param fdUsed the fdUsed to set
     */
    public void setFdUsed(int fdUsed) {
        this.fdUsed = fdUsed;
    }

    /**
     * @return the fdTotal
     */
    public int getFdTotal() {
        return fdTotal;
    }

    /**
     * @param fdTotal the fdTotal to set
     */
    public void setFdTotal(int fdTotal) {
        this.fdTotal = fdTotal;
    }

    /**
     * @return the memUsed
     */
    public int getMemUsed() {
        return memUsed;
    }

    /**
     * @param memUsed the memUsed to set
     */
    public void setMemUsed(int memUsed) {
        this.memUsed = memUsed;
    }

    /**
     * @return the memLimit
     */
    public int getMemLimit() {
        return memLimit;
    }

    /**
     * @param memLimit the memLimit to set
     */
    public void setMemLimit(int memLimit) {
        this.memLimit = memLimit;
    }

    /**
     * @return the procUsed
     */
    public int getProcUsed() {
        return procUsed;
    }

    /**
     * @param procUsed the procUsed to set
     */
    public void setProcUsed(int procUsed) {
        this.procUsed = procUsed;
    }

    /**
     * @return the procUotal
     */
    public int getProcTotal() {
        return procTotal;
    }

    /**
     * @param procTotal the procUotal to set
     */
    public void setProcRotal(int procTotal) {
        this.procTotal = procTotal;
    }

    /**
     * @return the erlangVersion
     */
    public String getErlangVersion() {
        return erlangVersion;
    }

    /**
     * @param erlangVersion the erlangVersion to set
     */
    public void setErlangVersion(String erlangVersion) {
        this.erlangVersion = erlangVersion;
    }

    /**
     * @return the uptime
     */
    public int getUptime() {
        return uptime;
    }

    /**
     * @param uptime the uptime to set
     */
    public void setUptime(int uptime) {
        this.uptime = uptime;
    }

    /**
     * @return the runQueue
     */
    public int getRunQueue() {
        return runQueue;
    }

    /**
     * @param runQueue the runQueue to set
     */
    public void setRunQueue(int runQueue) {
        this.runQueue = runQueue;
    }

    /**
     * @return the processors
     */
    public int getProcessors() {
        return processors;
    }

    /**
     * @param processors the processors to set
     */
    public void setProcessors(int processors) {
        this.processors = processors;
    }

    /**
     * @return the statisticsLevel
     */
    public String getStatisticsLevel() {
        return statisticsLevel;
    }

    /**
     * @param statisticsLevel the statisticsLevel to set
     */
    public void setStatisticsLevel(String statisticsLevel) {
        this.statisticsLevel = statisticsLevel;
    }

    /**
     * @return the applications
     */
    public List<Map<String, String>> getApplications() {
        return applications;
    }

    /**
     * @param aplications the applications to set
     */
    public void setApplications(List<Map<String, String>> aplications) {
        this.applications = aplications;
    }

    @Override
    public String toString() {
        return "RabbitNode{name=" + name + ", running=" + running + ", type=" + type + ", applications=" + applications
                + ", statisticsLevel=" + statisticsLevel + ", osPid=" + osPid + ", memEts=" + memEts
                + ", memBinary=" + memBinary + ", fdUsed=" + fdUsed + ", fdTotal=" + fdTotal + ", memUsed=" + memUsed
                + ", memLimit=" + memLimit + ", procUsed=" + procUsed + ", procUotal=" + procTotal
                + ", erlangVersion=" + erlangVersion + ", uptime=" + uptime + ", runQueue=" + runQueue
                + ", processors=" + processors + '}';
    }
}
