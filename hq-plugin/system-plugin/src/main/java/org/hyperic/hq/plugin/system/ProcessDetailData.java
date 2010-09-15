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

import java.util.List;
import java.util.Map;

import org.hyperic.sigar.ProcCred;
import org.hyperic.sigar.ProcExe;
import org.hyperic.sigar.ProcFd;
import org.hyperic.sigar.SigarProxy;
import org.hyperic.sigar.SigarException;

public class ProcessDetailData extends ProcessData {

    private String[] _procArgs;
    private Map _procEnv;
    private List _procModules;
    private ProcExe _procExe;
    private ProcFd _procFd;
    private ProcCred _procCred;

    public ProcessDetailData() {}
 
    //catch/ignore exceptions:
    //- if the pid is invalid super.populate will throw ex
    //- if any getProc* below throws ex it is due to
    //  permissions or method is not implemented for the given platform
    public void populate(SigarProxy sigar, long pid)
        throws SigarException {

        super.populate(sigar, pid);

        try {
            _procArgs = sigar.getProcArgs(pid);
        } catch (SigarException e) {}

        try {
            _procEnv = sigar.getProcEnv(pid);
        } catch (SigarException e) {}

        try {
            _procModules = sigar.getProcModules(pid);
        } catch (SigarException e) {}

        try {
            _procExe = sigar.getProcExe(pid);
        } catch (SigarException e) {}

        try {
            _procFd = sigar.getProcFd(pid);
        } catch (SigarException e) {}

        try {
            _procCred = sigar.getProcCred(pid);
        } catch (SigarException e) {}
    }

    public static ProcessData gather(SigarProxy sigar, long pid)
        throws SigarException {

        ProcessDetailData data = new ProcessDetailData();
        data.populate(sigar, pid);
        return data;
    }

    public String[] getProcArgs() {
        return _procArgs;
    }

    public Map getProcEnv() {
        return _procEnv;
    }

    public List getProcModules() {
        return _procModules;
    }

    public ProcExe getProcExe() {
        return _procExe;
    }

    public ProcFd getProcFd() {
        return _procFd;
    }

    public ProcCred getProcCred() {
        return _procCred;
    }
}
