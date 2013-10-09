/*
 * NOTE: This copyright does *not* cover user programs that use HQ
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of HQ.
 *
 * HQ is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */
//XXX move this class to sigar
package org.hyperic.hq.plugin.system;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcDiskIO;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.ProcUtil;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarProxy;

public class ProcessData {
    public static final String NA = "-";

    public static final String LABEL_PID = "PID";
    public static final String LABEL_USER = "USER";
    public static final String LABEL_STIME = "STIME";
    public static final String LABEL_SIZE = "SIZE";
    public static final String LABEL_RSS = "RSS";
    public static final String LABEL_SHARE = "SHARE";
    public static final String LABEL_STATE = "STATE";
    public static final String LABEL_TIME = "TIME";
    public static final String LABEL_CPU = "%CPU";
    public static final String LABEL_MEM = "%MEM";
    public static final String LABEL_NAME = "COMMAND";
    
    public static Pattern baseNamePattern = Pattern.compile("\\d+|\\d+:\\d+|u:\\d+");

    public static final String PS_HEADER = LABEL_PID + "\t" + LABEL_USER + "\t" + LABEL_STIME + "\t" + LABEL_SIZE
            + "\t" + LABEL_RSS + "\t" + LABEL_SHARE + "\t" + LABEL_STATE + "\t" + LABEL_TIME + "\t" + LABEL_CPU + "\t"
            + LABEL_MEM + "\t" + LABEL_NAME;

    private long _pid;
    private String _owner;
    private long _startTime;
    private long _size;
    private long _resident;
    private long _share;
    private char _state;
    private long _cpuTotal;
    private double _cpuPerc;
    private double _memPerc;
    private long _totalDiskBytes;
    private long _diskBytesRead;
    private long _diskBytesWritten;

    private String _name;
    private String[] _args;
    private Map _env;

    private ProcState _procState;
    private ProcCredName _procCredName;
    private ProcCpu _procCpu;
    private ProcMem _procMem;
    private ProcDiskIO _procDiskIO;

    public ProcessData() {
    }

    public void populate(SigarProxy sigar, long pid) throws SigarException {

        try {
            _args = sigar.getProcArgs(pid);
        } catch (SigarException e) {
            _args = new String[] {};
        }

        try {
            _env = sigar.getProcEnv(pid);
        } catch (SigarException e) {
            _env = new HashMap();
        }

        _procState = sigar.getProcState(pid);
        final String unknown = "???";

        _pid = pid;

        try {
            _procCredName = sigar.getProcCredName(pid);
            _owner = _procCredName.getUser();
        } catch (SigarException e) {
            _owner = unknown;
        }

        try {
            _procDiskIO = sigar.getProcDiskIO(pid);
            _totalDiskBytes = _procDiskIO.getBytesTotal();
            _diskBytesRead = _procDiskIO.getBytesRead();
            _diskBytesWritten = _procDiskIO.getBytesWritten();
        } catch (SigarException e) {
            _totalDiskBytes = _diskBytesRead = _diskBytesWritten = Sigar.FIELD_NOTIMPL;
        }

        try {
            _procCpu = sigar.getProcCpu(pid);
            _startTime = _procCpu.getStartTime();
        } catch (SigarException e) {
            _startTime = Sigar.FIELD_NOTIMPL;
        }

        try {
            _procMem = sigar.getProcMem(pid);
            _size = _procMem.getSize();
            _resident = _procMem.getResident();
            _share = _procMem.getShare();
            _memPerc = (double) _resident / sigar.getMem().getTotal();
        } catch (SigarException e) {
            _memPerc = _size = _resident = _share = Sigar.FIELD_NOTIMPL;
        }

        _state = _procState.getState();

        if (_procCpu != null) {
            _cpuTotal = _procCpu.getTotal();
            _cpuPerc = _procCpu.getPercent();
        } else {
            _cpuPerc = _cpuTotal = Sigar.FIELD_NOTIMPL;
        }

        _name = ProcUtil.getDescription(sigar, pid);
    }


    public static ProcessData gather(SigarProxy sigar, long pid) throws SigarException {

        ProcessData data = new ProcessData();
        data.populate(sigar, pid);
        return data;
    }

    public String[] getProcArgs() {
        return _args;
    }

    public Map getProcEnv() {
        return Collections.unmodifiableMap(_env);
    }

    public ProcState getProcState() {
        return _procState;
    }

    public ProcCredName getProcCredName() {
        return _procCredName;
    }

    public ProcCpu getProcCpu() {
        return _procCpu;
    }

    public ProcMem getProcMem() {
        return _procMem;
    }

    public long getPid() {
        return _pid;
    }

    public String getOwner() {
        return _owner;
    }

    public long getStartTime() {
        return _startTime;
    }

    public long getSize() {
        return _size;
    }

    public long getShare() {
        return _share;
    }

    public long getResident() {
        return _resident;
    }

    public char getState() {
        return _state;
    }

    public long getCpuTotal() {
        return _cpuTotal;
    }

    public double getCpuPerc() {
        return _cpuPerc;
    }

    public double getMemPerc() {
        return _memPerc;
    }

    public String getName() {
        return _name;
    }

    public String getBaseName() {
        int ix = _name.lastIndexOf("/");
        if (ix == -1) {
            ix = _name.lastIndexOf("\\");
        }
        if (ix == -1) {
            return _name;
        } else {
            String afterLastSlash = _name.substring(ix + 1).trim();
            if (StringUtils.isEmpty(afterLastSlash) || baseNamePattern.matcher(afterLastSlash).matches()) {
                // afterLastSlash is empty or of the form: digits, digits:digits, u:digits - return the whole _name
                return _name;
            }
            else {
                return afterLastSlash;
            }
        }
    }

    public String getFormattedStartTime() {
        return getFormattedStartTime(_startTime);
    }

    public static String getFormattedStartTime(long time) {
        if (time == 0) {
            return "00:00";
        } else if (time == Sigar.FIELD_NOTIMPL) {
            return NA;
        }

        long timeNow = System.currentTimeMillis();
        String fmt = "MMMd";

        if ((timeNow - time) < ((60 * 60 * 24) * 1000)) {
            fmt = "HH:mm";
        }

        return new SimpleDateFormat(fmt).format(new Date(time));
    }

    public String getFormattedSize() {
        return Sigar.formatSize(_size);
    }

    public String getFormattedShare() {
        return Sigar.formatSize(_share);
    }

    public String getFormattedResident() {
        return Sigar.formatSize(_resident);
    }

    public String getFormattedTotalDiskBytes() {
        return Sigar.formatSize(_totalDiskBytes);
    }

    public String getFormattedDiskReadBytes() {
        return Sigar.formatSize(_diskBytesRead);
    }

    public String getFormattedDiskWrittenBytes() {
        return Sigar.formatSize(_diskBytesWritten);
    }

    public long getTotalDiskBytes() {
        return _totalDiskBytes;
    }

    public long getdDiskReadBytes() {
        return _diskBytesRead;
    }

    public long getDiskWrittenBytes() {
        return _diskBytesWritten;
    }

    public String getFormattedCpuTotal() {
        return getFormattedCpuTotal(_cpuTotal);
    }

    public static String getFormattedCpuTotal(long total) {
        if (total == Sigar.FIELD_NOTIMPL) {
            return NA;
        }
        long t = total / 1000;
        String sec = String.valueOf(t % 60);
        if (sec.length() == 1) {
            sec = "0" + sec;
        }
        return (t / 60) + ":" + sec;
    }

    public String getFormattedCpuPerc() {
        if (_cpuPerc == Sigar.FIELD_NOTIMPL) {
            return NA;
        }
        return CpuPerc.format(_cpuPerc);
    }

    public String getFormattedMemPerc() {
        if (_memPerc == Sigar.FIELD_NOTIMPL) {
            return NA;
        }
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(1);
        percentFormat.setMinimumFractionDigits(1);
        return percentFormat.format(_memPerc);
    }

    public String toString(String delim) {
        return _pid + delim + _owner + delim + getFormattedStartTime() + delim + getFormattedSize() + delim
                + getFormattedResident() + delim + getFormattedShare() + delim + _state + delim
                + getFormattedCpuTotal() + delim + getFormattedCpuPerc() + delim + getFormattedMemPerc() + delim
                + getBaseName();
    }

    @Override
    public String toString() {
        return toString(",");
    }

}
