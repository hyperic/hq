package org.hyperic.hq.plugin.system;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

public class ProcessReport implements Externalizable {

    private static final long serialVersionUID = 1L;
    private long pid;
    private String owner;
    private long size;
    private long resident;
    private long share;
    private long cpuTotal;
    private double cpuPerc;
    private String baseName;
    private long startTime;

    public ProcessReport() {
    }

    public ProcessReport(ProcessData process) {
        this.pid = process.getPid();
        this.owner = process.getOwner();
        this.size = process.getSize();
        this.resident = process.getResident();
        this.share = process.getShare();
        this.cpuTotal = process.getCpuTotal();
        this.cpuPerc = process.getCpuPerc();
        this.baseName = process.getBaseName();
        this.startTime = process.getStartTime();
    }

    public long getPid() {
        return pid;
    }

    public void setPid(long pid) {
        this.pid = pid;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getResident() {
        return resident;
    }

    public void setResident(long resident) {
        this.resident = resident;
    }

    public long getShare() {
        return share;
    }

    public void setShare(long share) {
        this.share = share;
    }

    public double getCpuPerc() {
        return cpuPerc;
    }

    public void setCpuPerc(double cpuPerc) {
        this.cpuPerc = cpuPerc;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public long getCpuTotal() {
        return cpuTotal;
    }

    public void setCpuTotal(long cpuTotal) {
        this.cpuTotal = cpuTotal;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(pid);
        out.writeUTF(owner);
        out.writeLong(size);
        out.writeLong(resident);
        out.writeLong(share);
        out.writeLong(cpuTotal);
        out.writeDouble(cpuPerc);
        out.writeUTF(baseName);
        out.writeLong(startTime);
    }

    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        pid = in.readLong();
        owner = in.readUTF();
        size = in.readLong();
        resident = in.readLong();
        share = in.readLong();
        cpuTotal = in.readLong();
        cpuPerc = in.readDouble();
        baseName = in.readUTF();
        startTime = in.readLong();
    }

    @Override
    public String toString() {
        return "ProcessReport [pid=" + pid + ", owner=" + owner + ", size=" + size + ", resident=" + resident
                + ", share=" + share + ", cpuTotal=" + cpuTotal + ", cpuPerc=" + cpuPerc + ", baseName=" + baseName
                + ", startTime=" + startTime + "]";
    }



}
