package org.hyperic.hq.plugin.system;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;

public class TopReport implements Externalizable {

    private static final long serialVersionUID = 1L;
    private long createTime;
    private String upTime;
    private String cpu;
    private String mem;
    private String swap;
    private Set<ProcessReport> processes = new HashSet<ProcessReport>();

    public TopReport() {
    }


    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long creatTime) {
        this.createTime = creatTime;
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



    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeLong(createTime);
        out.writeUTF(upTime);
        out.writeUTF(cpu);
        out.writeUTF(mem);
        out.writeUTF(swap);
        out.writeObject(processes);

    }

    @SuppressWarnings("unchecked")
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        createTime = in.readLong();
        upTime = in.readUTF();
        cpu = in.readUTF();
        mem = in.readUTF();
        swap = in.readUTF();
        processes = ((Set<ProcessReport>) in.readObject());
    }

    public byte[] toSerializedForm() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ObjectOutputStream objStream = new ObjectOutputStream(outputStream);
        objStream.writeObject(this);
        objStream.close();
        return outputStream.toByteArray();
    }

    public static final TopReport fromSerializedForm(final byte[] data, final int startPos,
                                                     final int length) throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inStream = new ByteArrayInputStream(data, startPos, length);
        final ObjectInputStream objStream = new ObjectInputStream(inStream);
        return (TopReport) objStream.readObject();
    }

    public static final TopReport fromSerializedForm(final byte[] data) throws IOException, ClassNotFoundException {
        return fromSerializedForm(data, 0, data.length);
    }

    @Override
    public String toString() {
        return "TopReport [createTime=" + createTime + ", upTime=" + upTime + ", cpu=" + cpu + ", mem=" + mem + ", " +
                "swap="
                + swap + ", processes=" + processes + "]";
    }


    /*public void insertTopN(List topNData) throws InterruptedException{
        throw new NotImplementedException();
    } */

}
