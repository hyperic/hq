package org.hyperic.hq.plugin.system;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TopReport implements Serializable {

    public enum TOPN_SORT_TYPE {
        CPU, MEM;
    }

    private static final long serialVersionUID = 1L;
    private long createTime;
    private String upTime;
    private String procStat;
    private String cpu;
    private String mem;
    private String swap;
    private Set<ProcessReport> processes = new HashSet<ProcessReport>();

    public TopReport() {
    }


    public void filterTopProcesses(final int topNumber) {
        if (processes.size() <= topNumber) {
            return;
        }
        List<ProcessReport> processesList = new ArrayList<ProcessReport>(processes);
        processes.clear();

        sortProcessesByCpu(processesList);
        processes.addAll(processesList.subList(0, topNumber - 1));

        sortProcessesByMemory(processesList);
        processes.addAll(processesList.subList(0, topNumber - 1));

    }

    private void sortProcessesByMemory(List<ProcessReport> processesList) {
        Collections.sort(processesList, new Comparator<ProcessReport>() {
            public int compare(ProcessReport first, ProcessReport second) {
                double firstMem = -1;
                double secondMem = -1;

                if (first.getMemPerc().contains("%")) {
                    firstMem = Double.valueOf(first.getMemPerc().replace("%", "").trim());
                }
                if (second.getMemPerc().contains("%")) {
                    secondMem = Double.valueOf(second.getMemPerc().replace("%", "").trim());
                }
                return Double.compare(firstMem, secondMem) * (-1);
            }
        });
    }

    private void sortProcessesByCpu(List<ProcessReport> processesList) {
        Collections.sort(processesList, new Comparator<ProcessReport>() {
            public int compare(ProcessReport first, ProcessReport second) {
                double firstCpu = -1;
                double secondCpu = -1;

                if (first.getCpuPerc().contains("%")) {
                    firstCpu = Double.valueOf(first.getCpuPerc().replace("%", "").trim());
                }
                if (second.getCpuPerc().contains("%")) {
                    secondCpu = Double.valueOf(second.getCpuPerc().replace("%", "").trim());
                }
                return Double.compare(firstCpu, secondCpu) * (-1);
            }
        });
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

    public void setProcStat(String procStat) {
        this.procStat = procStat;
    }

    public String getProcStat() {
        return procStat;
    }

    public List<ProcessReport> getProcessesSorted(TOPN_SORT_TYPE type) {
        List<ProcessReport> processesList = new ArrayList<ProcessReport>(processes);
        switch (type) {
        case CPU:
            sortProcessesByCpu(processesList);
            break;
        case MEM:
            sortProcessesByMemory(processesList);
            break;
        default:
            break;
        }
        return processesList;
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
        createTime = in.readLong();
        upTime = in.readUTF();
        cpu = in.readUTF();
        mem = in.readUTF();
        swap = in.readUTF();
        procStat = in.readUTF();
        processes = (Set<ProcessReport>) in.readObject();
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeLong(createTime);
        out.writeUTF(upTime);
        out.writeUTF(cpu);
        out.writeUTF(mem);
        out.writeUTF(swap);
        out.writeUTF(procStat);
        out.writeObject(processes);
    }

    public byte[] toSerializedForm() throws IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        final ObjectOutputStream objStream = new ObjectOutputStream(outputStream);
        objStream.writeObject(this);
        objStream.close();
        return outputStream.toByteArray();
    }

    public static final TopReport fromSerializedForm(final byte[] data, final int startPos,
 final int length)
            throws IOException, ClassNotFoundException {
        final ByteArrayInputStream inStream = new ByteArrayInputStream(data, startPos, length);
        final ObjectInputStream objStream = new ObjectInputStream(inStream);
        return (TopReport) objStream.readObject();
    }

    public static final TopReport fromSerializedForm(final byte[] data) throws IOException, ClassNotFoundException {
        return fromSerializedForm(data, 0, data.length);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("TopReport\n").append("------------------------------\n").append("createTime=").append(createTime)
                .append("\n").append("upTime=").append(upTime).append("\n").append("cpu=").append(cpu).append("\n")
                .append("mem=").append(mem).append("\n").append("swap=").append(swap).append("\n\n")
                .append("processes\n").append("------------------------------\n");
        for (ProcessReport process : processes) {
            sb.append(process.toString()).append("\n");
        }
        return sb.toString();
    }

}
