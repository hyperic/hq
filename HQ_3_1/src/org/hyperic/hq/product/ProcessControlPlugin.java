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

package org.hyperic.hq.product;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.SigarLoader;
import org.hyperic.sigar.ptql.ProcessFinder;
import org.hyperic.util.config.ConfigResponse;

public class ProcessControlPlugin extends ControlPlugin {

    public static final String KILL    = "kill";
    public static final String SIGHUP  = "SIGHUP";
    public static final String SIGINT  = "SIGINT";
    public static final String SIGQUIT = "SIGQUIT";
    public static final String SIGKILL = "SIGKILL";
    public static final String SIGTERM = "SIGTERM";
    public static final String SIGUSR1 = "SIGUSR1";
    public static final String SIGUSR2 = "SIGUSR2";
    
    public static final List ACTIONS =
        Arrays.asList(new String[] {
            KILL,
            SIGHUP, SIGINT, SIGQUIT,
            SIGKILL, SIGTERM,
            SIGUSR1, SIGUSR2
    });

    private static final Map SIGNALS = new HashMap();

    private static final int POSIX_SIGHUP  = 1;
    private static final int POSIX_SIGINT  = 2;
    private static final int POSIX_SIGQUIT = 3;
    private static final int POSIX_SIGKILL = 9;
    private static final int POSIX_SIGTERM = 15;

    private String _ptql;

    public void init(PluginManager manager)
        throws PluginException {

        super.init(manager);

        if (SIGNALS.size() != 0) {
            return;
        }
        //XXX sigar 1.4 will have Sigar.getSigNum
        SIGNALS.put(SIGINT, new Integer(POSIX_SIGINT));
        SIGNALS.put(SIGTERM, new Integer(POSIX_SIGTERM));

        if (!SigarLoader.IS_WIN32) {
            SIGNALS.put(SIGHUP, new Integer(POSIX_SIGHUP));
            SIGNALS.put(SIGQUIT, new Integer(POSIX_SIGQUIT));
            SIGNALS.put(SIGKILL, new Integer(POSIX_SIGKILL));
        }

        if (SigarLoader.IS_AIX ||
            SigarLoader.IS_DARWIN ||
            SigarLoader.IS_FREEBSD)
        {
            SIGNALS.put(SIGUSR1, new Integer(30));
            SIGNALS.put(SIGUSR2, new Integer(31));
        }
        else if (SigarLoader.IS_SOLARIS ||
                 SigarLoader.IS_HPUX)
        {
            SIGNALS.put(SIGUSR1, new Integer(16));
            SIGNALS.put(SIGUSR2, new Integer(17));
        }
        else if (SigarLoader.IS_LINUX) {
            SIGNALS.put(SIGUSR1, new Integer(10));
            SIGNALS.put(SIGUSR2, new Integer(12));
        }
        else if (SigarLoader.IS_WIN32) {
            SIGNALS.put(SIGKILL, new Integer(POSIX_SIGTERM));   
        }
    }

    public void configure(ConfigResponse config)
        throws PluginException {

        super.configure(config);

        _ptql =
            config.getValue(SigarMeasurementPlugin.PTQL_CONFIG);
    }

    public List getActions() {
        return ACTIONS;
    }

    //override for MultiProc support
    protected boolean killAll() {
        return false;
    }

    public static int getSignal(String signal)
        throws PluginException {

        if (Character.isDigit(signal.charAt(0))) {
            try {
                return Integer.parseInt(signal);
            } catch (NumberFormatException e) {
                throw new PluginException(signal + ": " + e);
            }
        }
        else {
            Integer num = (Integer)SIGNALS.get(signal);
            if (num == null) {
                num = (Integer)SIGNALS.get("SIG" + signal);
            }
            if (num == null) {
                throw new PluginException("Invalid signal: " + signal);
            }
            return num.intValue();
        }
    }

    public void doAction(String action, String[] args)
        throws PluginException {

        String signal;

        if (action.equals(KILL)) {
            if (args.length == 0) {
                signal = SIGKILL;
            }
            else if (args.length == 1) {
                signal = args[0].toUpperCase();
            }
            else {
                throw new PluginException("Too many arguments");
            }
        }
        else if (args.length == 0) {
            signal = action;
            if (signal.startsWith("-")) { //habit
                signal = signal.substring(1);
            }
        }
        else {
            throw new PluginException("Too many arguments");
        }

        int signum = getSignal(signal);
        Sigar sigar = new Sigar();
        long[] pids;
        List killed = new ArrayList();

        try {
            pids =
                ProcessFinder.find(sigar, _ptql);
            if (pids.length == 0) {
                setResult(ControlPlugin.RESULT_FAILURE);
                setMessage("No processes match query: " +
                           _ptql);
                return;
            }
            else if ((pids.length > 1) && !killAll()) {
                setMessage(pids.length + " processes match query: " +
                           _ptql);
                return;
            }
        } catch (SigarException e) {
            sigar.close();
            throw new PluginException(_ptql + ": " + e);
        }

        for (int i=0; i<pids.length; i++) {
            try {
                sigar.kill(pids[i], signum);
                killed.add(new Long(pids[i]));
            } catch (SigarException e) {
                setResult(ControlPlugin.RESULT_FAILURE);
                setMessage(e.getMessage());
            }
        }

        sigar.close();
        setResult(ControlPlugin.RESULT_SUCCESS);
        setMessage("kill " + signal + " " + killed);
    }
}
