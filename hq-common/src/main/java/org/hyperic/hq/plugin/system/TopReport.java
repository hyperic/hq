package org.hyperic.hq.plugin.system;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class TopReport implements Serializable {

    private static final long serialVersionUID = 1L;
    private long creatTime;
    private String upTime;
    private String cpu;
    private String mem;
    private String swap;
    private Set<ProcessReport> processes = new HashSet<ProcessReport>();

    public TopReport() {
    }


    public long getCreatTime() {
        return creatTime;
    }

    public void setCreatTime(long creatTime) {
        this.creatTime = creatTime;
    }

    public String getUpTime() {
        return upTime;
    }

    public void setUpTime(String upTime) {
        this.upTime = upTime;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    public String getMem() {
        return mem;
    }

    public void setMem(String mem) {
        this.mem = mem;
    }

    public String getSwap() {
        return swap;
    }

    public void setSwap(String swap) {
        this.swap = swap;
    }

    public Set<ProcessReport> getProcesses() {
        return processes;
    }

    public void setProcesses(Set<ProcessReport> processes) {
        this.processes = processes;
    }

    public void addProcess(ProcessReport process) {
        this.processes.add(process);
    }

    @SuppressWarnings("unchecked")
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        creatTime = in.readLong();
        upTime = in.readUTF();
        cpu = in.readUTF();
        mem = in.readUTF();
        swap = in.readUTF();
        processes = (Set<ProcessReport>) in.readObject();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeLong(creatTime);
        out.writeUTF(upTime);
        out.writeUTF(cpu);
        out.writeUTF(mem);
        out.writeUTF(swap);
        out.writeObject(processes);
    }

    @Override
    public String toString() {
        return "TopReport [creatTime=" + creatTime + ", upTime=" + upTime + ", cpu=" + cpu + ", mem=" + mem + ", swap="
                + swap + ", processes=" + processes + "]";
    }

}
