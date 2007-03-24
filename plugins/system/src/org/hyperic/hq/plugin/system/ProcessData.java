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

import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcState;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.ProcUtil;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

public class ProcessData {

    private long _pid;
    private String _owner;
    private long _startTime;
    private long _size;
    private long _resident;
    private long _share;
    private char _state;
    private long _cpuTotal;
    private double _cpuPerc;
    private String _name;

    public ProcessData() {}

    public void populate(Sigar sigar, long pid)
        throws SigarException {

        ProcState state = sigar.getProcState(pid);
        ProcTime time = null;
        final String unknown = "???";

        _pid = pid;

        try {
            ProcCredName cred = sigar.getProcCredName(pid);
            _owner = cred.getUser();
        } catch (SigarException e) {
            _owner = unknown;
        }

        try {
            time = sigar.getProcTime(pid);
            _startTime = time.getStartTime();
        } catch (SigarException e) {
           _startTime = Sigar.FIELD_NOTIMPL;
        }

        try {
            ProcMem mem = sigar.getProcMem(pid);
            _size = mem.getSize();
            _resident = mem.getResident();
            _share = mem.getShare();
        } catch (SigarException e) {
            _size = _resident = _share = Sigar.FIELD_NOTIMPL;
        }

        _state = state.getState();

        if (time != null) {
            _cpuTotal = time.getTotal();
        }
        else {
            _cpuTotal = Sigar.FIELD_NOTIMPL;
        }

        try {
            ProcCpu cpu = sigar.getProcCpu(pid);
            _cpuPerc = cpu.getPercent();
        } catch (SigarException e) {
            _cpuPerc = Sigar.FIELD_NOTIMPL;
        }

        _name = ProcUtil.getDescription(sigar, pid);
    }

    public static ProcessData gather(Sigar sigar, long pid)
        throws SigarException {

        ProcessData data = new ProcessData();
        data.populate(sigar, pid);
        return data;
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

    public String getName() {
        return _name;
    }
}
