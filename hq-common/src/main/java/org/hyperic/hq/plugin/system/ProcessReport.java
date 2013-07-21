package org.hyperic.hq.plugin.system;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class ProcessReport implements Serializable {

    private static final long serialVersionUID = 1L;
    private long pid;
    private String owner;
    private String size;
    private String resident;
    private String share;
    private String cpuTotal;
    private String cpuPerc;
    private String memPerc;
    private char state;
    private String baseName;
    private String startTime;
    private String[] args;

    public ProcessReport() {
    }

    public ProcessReport(ProcessData process) {
        this.pid = process.getPid();
        this.owner = process.getOwner();
        this.startTime = process.getFormattedStartTime();
        this.size = process.getFormattedSize();
        this.resident = process.getFormattedResident();
        this.share = process.getFormattedShare();
        this.cpuTotal = process.getFormattedCpuTotal();
        this.cpuPerc = process.getFormattedCpuPerc();
        this.memPerc = process.getFormattedMemPerc();
        this.baseName = process.getBaseName();
        this.state = process.getState();

       this.setArgs(process.getProcArgs());
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

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getResident() {
        return resident;
    }

    public void setResident(String resident) {
        this.resident = resident;
    }

    public String getShare() {
        return share;
    }

    public void setShare(String share) {
        this.share = share;
    }

    public String getCpuPerc() {
        return cpuPerc;
    }

    public void setCpuPerc(String cpuPerc) {
        this.cpuPerc = cpuPerc;
    }

    public String getMemPerc() {
        return memPerc;
    }

    public void setMemPerc(String memPerc) {
        this.memPerc = memPerc;
    }

    public String getBaseName() {
        return baseName;
    }

    public void setBaseName(String baseName) {
        this.baseName = baseName;
    }

    public String getCpuTotal() {
        return cpuTotal;
    }

    public void setCpuTotal(String cpuTotal) {
        this.cpuTotal = cpuTotal;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public char getState() {
        return state;
    }

    public void setState(char state) {
        this.state = state;
    }

    public String[] getArgs() {
        return args;
    }

    public void setArgs(String[] args) {
        this.args = args;
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        pid = in.readLong();
        owner = in.readUTF();
        size = in.readUTF();
        resident = in.readUTF();
        share = in.readUTF();
        cpuTotal = in.readUTF();
        cpuPerc = in.readUTF();
        memPerc = in.readUTF();
        baseName = in.readUTF();
        startTime = in.readUTF();
        state = in.readChar();
        args = (String[]) in.readObject();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeLong(pid);
        out.writeUTF(owner);
        out.writeUTF(size);
        out.writeUTF(resident);
        out.writeUTF(share);
        out.writeUTF(cpuTotal);
        out.writeUTF(cpuPerc);
        out.writeUTF(memPerc);
        out.writeUTF(baseName);
        out.writeUTF(startTime);
        out.writeChar(state);
        out.writeObject(args);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("pid=").append(pid).append(", owner=").append(owner).append(", size=").append(size).append(", " +
                "" + "resident=").append(resident).append(", share=").append(share).append(", " +
                "" + "cpuTotal=").append(cpuTotal).append(", cpuPerc=").append(cpuPerc).append(", " +
                "" + "memPerc=").append(memPerc).append(", baseName = ").append(baseName).append(", " +
                "" + "startTime = ").append(startTime).append(", args = ");
        for (String arg : args) {
            if (!arg.equalsIgnoreCase("")) {
                sb.append(arg).append(",");
            }
        }
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
