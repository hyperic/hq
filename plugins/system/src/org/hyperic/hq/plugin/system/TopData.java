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

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.hyperic.sigar.CpuPerc;
import org.hyperic.sigar.CurrentProcessSummary;
import org.hyperic.sigar.Mem;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Swap;
import org.hyperic.sigar.ptql.ProcessFinder;

public class TopData {

    private UptimeData _uptime;
    private CurrentProcessSummary _currentProcessSummary;
    private CpuPerc _cpu;
    private Mem _mem;
    private Swap _swap;
    private List _processes;

    public TopData() {}

    public void populate(Sigar sigar, String filter)
        throws SigarException {

        _uptime = UptimeData.gather(sigar);
        _currentProcessSummary = CurrentProcessSummary.get(sigar);
        _cpu = sigar.getCpuPerc();
        _mem = sigar.getMem();
        _swap = sigar.getSwap();

        ps(sigar, filter);
    }

    private void ps(Sigar sigar, String filter) throws SigarException {
        _processes = new ArrayList();
        long[] pids;

        if (filter == null) {
            pids = sigar.getProcList();
        }
        else {
            pids = ProcessFinder.find(sigar, filter);
        }

        for (int i=0; i<pids.length; i++) {
            long pid = pids[i];
            try {
                _processes.add(ProcessData.gather(sigar, pid));
            } catch (SigarException e) {
                
            }
        }
    }

    public static TopData gather(Sigar sigar, String filter)
        throws SigarException {

        TopData data = new TopData();
        data.populate(sigar, filter);
        return data;
    }

    public UptimeData getUptime() {
        return _uptime;
    }

    public CurrentProcessSummary getCurrentProcessSummary() {
        return _currentProcessSummary;
    }

    public CpuPerc getCpu() {
        return _cpu;
    }

    public Mem getMem() {
        return _mem;
    }

    public Swap getSwap() {
        return _swap;
    }

    public List getProcesses() {
        return _processes;
    }

    public void print(PrintStream out) {
        out.println(getUptime());
        out.println(getCurrentProcessSummary());
        out.println(getCpu());
        out.println(getMem());
        out.println(getSwap());
        out.println();
        out.println(ProcessData.PS_HEADER);
        List processes = getProcesses();
        for (int i=0; i<processes.size(); i++) {
            ProcessData process = (ProcessData)processes.get(i);
            out.println(process.toString("\t"));
        }
    }

    public static void main(String[] args) throws Exception {
        String filter;
        if (args.length == 1) {
            filter = args[0];
        }
        else {
            filter = null;
        }

        Sigar sigar = new Sigar();
        TopData top = TopData.gather(sigar, filter);
        top.print(System.out);
        sigar.close();
    }
}
